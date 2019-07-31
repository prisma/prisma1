//! Prisma Response IR (Intermediate Representation).
//!
//! This module takes care of processing the results
//! and transforming them into a different AST.
//!
//! This IR is meant for general processing and storage.
//! It can also be easily serialized.
use crate::{
    CoreError, CoreResult, EnumTypeRef, IntoArc, ObjectTypeStrongRef, OutputType, OutputTypeRef, ResultPair, ScalarType,
};
use connector::{QueryArguments, QueryResult, ReadQueryResult, ScalarListValues, WriteQueryResult};
use indexmap::IndexMap;
use prisma_models::{GraphqlId, PrismaValue};
use std::{borrow::Borrow, collections::HashMap, convert::TryFrom, sync::Arc};

/// A `key -> value` map to an IR item
pub type Map = IndexMap<String, Item>;

/// A list of IR items
pub type List = Vec<Item>;

/// Convenience type wrapper for Arc<Item>.
pub type ItemRef = Arc<Item>;

/// A response can either be some `key-value` data representation
/// or an error that occured.
#[derive(Debug)]
pub enum Response {
    Data(String, Item),
    Error(String),
}

/// An IR item that either expands to a subtype or leaf-record.
#[derive(Debug, Clone)]
pub enum Item {
    Map(Map),
    List(List),
    Value(PrismaValue),

    /// Wrapper type to allow multiple parent records
    /// to claim the same item without copying data
    /// (serialization can then choose how to copy if necessary).
    Ref(ItemRef),
}

// impl Item {
//     pub fn is_null_or_empty(&self) -> bool {
//         match self {
//             Item::Value(PrismaValue::Null) => true,
//             Item::Map(map) => map.iter().find(|(k, v)| !v.is_null_or_empty()).is_none(),
//             Item::List(l) => l.is_empty() || l.iter().find(|el| !el.is_null_or_empty()).is_none(),
//             _ => false,
//         }
//     }
// }

/// A grouping of items to their parent record.
/// The item implicitly holds the information of the type of item contained.
/// E.g., if the output type of a field designates a single object, the item will be
/// Item::Map(map), if it's a list, Item::List(list), etc. (hence "checked")
type CheckedItemsWithParents = IndexMap<Option<GraphqlId>, Item>;

/// A grouping of items to their parent record.
/// As opposed to the checked mapping, this map isn't holding final information about
/// the contained items, i.e. the Items are all unchecked.
type UncheckedItemsWithParents = IndexMap<Option<GraphqlId>, Vec<Item>>;

/// An IR builder utility
#[derive(Debug)]
pub struct ResultIrBuilder(Vec<ResultPair>);

impl ResultIrBuilder {
    pub fn new() -> Self {
        Self(vec![])
    }

    /// Add a single query result to the builder
    pub fn add(mut self, q: ResultPair) -> Self {
        self.0.push(q);
        self
    }

    /// Parse collected queries into the return wrapper type
    pub fn build(self) -> Vec<Response> {
        self.0
            .into_iter()
            .fold(vec![], |mut vec, res| {
                match res {
                    ResultPair::Read(r, typ) => {
                        let name = r.alias.clone().unwrap_or_else(|| r.name.clone());
                        let serialized = Self::serialize_read(r, &typ, false, false);

                        match serialized {
                            Ok(result) => {
                                // On the top level, each result pair boils down to a exactly a single serialized result.
                                // All checks for lists and optionals have already been performed during the recursion,
                                // so we just unpack the only result possible.
                                let (_, item) = result.into_iter().take(1).next().unwrap();
                                vec.push(Response::Data(name, item));
                            }
                            Err(err) => vec.push(Response::Error(format!("{}", err))),
                        };
                    }
                    _ => unimplemented!(),
                };

                vec
            })
            .into_iter()
            .collect()
    }

    /// The query validation makes sure that the output selection already has the correct shape.
    /// This means that we can make the following assumptions:
    /// - Objects don't need to check required fields.
    /// - Objects don't need to check extra fields - just pick the selected ones and ignore the rest.
    ///
    /// The output validation has to make sure that returned values:
    /// - Are of the correct type.
    /// - Are nullable if not present.
    ///
    /// The is_list and is_optional flags dictate how object checks are done.
    /// // todo more here
    ///
    /// Returns a pair of (parent ID, response)
    fn serialize_read(
        result: ReadQueryResult,
        typ: &OutputTypeRef,
        is_list: bool,
        is_optional: bool,
    ) -> CoreResult<CheckedItemsWithParents> {
        let query_args = result.query_arguments.clone();
        let name = result.name.clone();

        match typ.borrow() {
            OutputType::List(inner) => Self::serialize_read(result, inner, true, false), // List resets optionals TODO document in details why
            OutputType::Opt(inner) => Self::serialize_read(result, inner, is_list, true),
            OutputType::Object(obj) => {
                let result = Self::serialize_objects(result, obj.into_arc())?;

                // Items will be ref'ed on the top level to allow cheap clones in nested scenarios.
                match (is_list, is_optional) {
                    // List(Opt(_)) | List(_)
                    (true, opt) => {
                        result
                            .into_iter()
                            .map(|(parent, mut items)| {
                                if !opt {
                                    // Check that all items are non-null
                                    if let Some(_) = items.iter().find(|item| match item {
                                        Item::Value(PrismaValue::Null) => true,
                                        _ => false,
                                    }) {
                                        return Err(CoreError::SerializationError(format!(
                                            "Required field '{}' returned a null record",
                                            name
                                        )));
                                    }
                                }

                                // Trim excess records
                                trim_records(&mut items, &query_args);
                                Ok((parent, Item::Ref(ItemRef::new(Item::List(items)))))
                            })
                            .collect()
                    }

                    // Opt(_)
                    (false, opt) => {
                        result
                            .into_iter()
                            .map(|(parent, mut items)| {
                                // As it's not a list, we require a single result
                                if items.len() > 1 {
                                    Err(CoreError::SerializationError(format!(
                                        "Expected at most 1 item for '{}', got {}",
                                        name,
                                        items.len()
                                    )))
                                } else if items.is_empty() && opt {
                                    Ok((parent, Item::Ref(ItemRef::new(Item::Value(PrismaValue::Null)))))
                                } else if items.is_empty() && opt {
                                    Err(CoreError::SerializationError(format!(
                                        "Required field '{}' returned a null record",
                                        name
                                    )))
                                } else {
                                    Ok((parent, Item::Ref(ItemRef::new(items.pop().unwrap()))))
                                }
                            })
                            .collect()
                    }
                }
            }

            _ => unreachable!(), // We always serialize reads into objects or lists on the top levels. Scalars and enums are handled separately.
        }
    }

    /// Serializes the given result into objects of given type.
    /// Doesn't validate the shape of the result set ("unchecked" result).
    /// Returns a vector of serialized objects (as Item::Map), grouped into a map by parent, if present.
    fn serialize_objects(
        mut result: ReadQueryResult,
        typ: ObjectTypeStrongRef,
    ) -> CoreResult<UncheckedItemsWithParents> {
        // The way our query execution works, we only need to look at nested + lists if we hit an object.
        // Move lists and nested out of result for separate processing.
        let nested = std::mem::replace(&mut result.nested, vec![]);
        let lists = std::mem::replace(&mut result.lists, vec![]);

        // { <nested field name> -> { parent ID -> items } }
        let mut nested_mapping: HashMap<String, CheckedItemsWithParents> = Self::process_nested_results(nested, &typ)?;

        // We need the Arcs to solve the issue where we have multiple parents claiming the same data (we want to move the data out of the nested structure
        // to prevent expensive copying during serialization).

        // { <list field name> -> { parent ID -> items } }
        let mut list_mapping = Self::process_scalar_lists(lists, &typ)?;

        // Finally, serialize the objects based on the selected fields.
        let mut object_mapping = UncheckedItemsWithParents::new();
        let scalar_field_names = result.scalars.field_names;

        // Write all fields, nested and list fields unordered into a map, afterwards order all into the final order.
        // If nothing is written to the object, write null instead.
        for record in result.scalars.records {
            let record_id = Some(record.collect_id(&scalar_field_names, &result.id_field)?);

            if !object_mapping.contains_key(&record.parent_id) {
                object_mapping.insert(record.parent_id.clone(), vec![]);
            }

            let mut object: HashMap<String, Item> = HashMap::new();

            // Write scalars, but skip objects and lists, which while they are in the selection, are handled separately.
            let values = record.values;
            for (val, field_name) in values.into_iter().zip(scalar_field_names.iter()) {
                let field = typ.find_field(field_name).unwrap();
                if !field.field_type.is_object() && !field.field_type.is_list() {
                    object.insert(field_name.to_owned(), Self::serialize_scalar(val, &field.field_type)?);
                }
            }

            // Write nested results
            nested_mapping.iter_mut().for_each(|(field_name, inner)| {
                let val = inner.get(&record_id).unwrap(); //.unwrap_or_else(|| Item::List(vec![])); // todo we don't want default empty here, do we?
                                                          // The value must be a reference, everything else is an error in the serialization logic.
                match val {
                    Item::Ref(ref r) => object.insert(field_name.to_owned(), Item::Ref(ItemRef::clone(r))),
                    _ => panic!("Application logic invariant error: Nested items have to be wrapped as a Item::Ref."),
                };
            });

            // Write scalar list results
            list_mapping.iter_mut().for_each(|(field_name, inner)| {
                let val = inner.get(&record_id).unwrap();
                // Same as nested, the value must be a reference.
                match val {
                    Item::Ref(ref r) => object.insert(field_name.to_owned(), Item::Ref(ItemRef::clone(r))),
                    _ => panic!("Application logic invariant error: Nested items have to be wrapped as a Item::Ref."),
                };
            });

            // Reorder into final shape.
            let mut map = Map::new();
            result.fields.iter().for_each(|field_name| {
                map.insert(field_name.to_owned(), object.remove(field_name).unwrap());
            });

            // TODO: Find out how to easily determine when a result is null.
            // If the object is null or completely empty, coerce into null instead.
            let result = Item::Map(map);
            // let result = if result.is_null_or_empty() {
            //     Item::Value(PrismaValue::Null)
            // } else {
            //     result
            // };

            object_mapping.get_mut(&record.parent_id).unwrap().push(result);
        }

        Ok(object_mapping)
    }

    /// Processes nested results into a more ergonomic structure of { <nested field name> -> { parent ID -> item (list, map, ...) } }.
    fn process_nested_results(
        nested: Vec<ReadQueryResult>,
        enclosing_type: &ObjectTypeStrongRef,
    ) -> CoreResult<HashMap<String, CheckedItemsWithParents>> {
        // For each nested selected field we need to map the parents to their items.
        let mut nested_mapping = HashMap::new();

        // Parse and validate all nested objects with their respective output type.
        // Unwraps are safe due to query validation.
        for nested_result in nested {
            let name = nested_result.name.clone();
            let field = enclosing_type.find_field(&name).unwrap();
            let result = Self::serialize_read(nested_result, &field.field_type, false, false)?;

            nested_mapping.insert(name, result);
        }

        Ok(nested_mapping)
    }

    /// Processes scalar lists into a more ergonomic structure of { <list field name> -> { parent ID -> item (Item::Ref) } }
    fn process_scalar_lists(
        lists: Vec<(String, Vec<ScalarListValues>)>,
        enclosing_type: &ObjectTypeStrongRef,
    ) -> CoreResult<HashMap<String, CheckedItemsWithParents>> {
        // For each selected scalar list field we need to map the parents to their items.
        let mut list_mapping: HashMap<String, CheckedItemsWithParents> = HashMap::new();
        for list_result in lists {
            let field = enclosing_type.find_field(&list_result.0).unwrap();

            // Todo optional lists...?
            let list_type = match field.field_type.borrow() {
                OutputType::List(inner) => inner,
                other => {
                    return Err(CoreError::SerializationError(format!(
                        "Attempted to serialize scalar list '{}' with non-scalar-list compatible type '{:?}'",
                        field.name.clone(),
                        other
                    )))
                }
            };

            list_mapping.insert(field.name.clone(), CheckedItemsWithParents::new());

            for list_pair in list_result.1 {
                let converted: Vec<Item> = list_pair
                    .values
                    .into_iter()
                    .map(|val| Self::serialize_scalar(val, &list_type))
                    .collect::<CoreResult<Vec<_>>>()?;

                list_mapping.get_mut(&field.name).unwrap().insert(
                    Some(list_pair.record_id),
                    Item::Ref(ItemRef::new(Item::List(converted))),
                );
            }
        }

        Ok(list_mapping)
    }

    fn serialize_scalar(value: PrismaValue, typ: &OutputTypeRef) -> CoreResult<Item> {
        match (&value, typ.borrow()) {
            (PrismaValue::Null, OutputType::Opt(_)) => Ok(Item::Value(PrismaValue::Null)),
            (_, OutputType::Opt(inner)) => Self::serialize_scalar(value, inner),
            (_, OutputType::Scalar(st)) => {
                let item_value = match (st, value) {
                    (ScalarType::String, PrismaValue::String(s)) => PrismaValue::String(s),

                    (ScalarType::ID, PrismaValue::GraphqlId(id)) => PrismaValue::GraphqlId(id),
                    (ScalarType::ID, val) => PrismaValue::GraphqlId(GraphqlId::try_from(val)?),

                    (ScalarType::Int, PrismaValue::Float(f)) => PrismaValue::Int(f as i64),
                    (ScalarType::Int, PrismaValue::Int(i)) => PrismaValue::Int(i),

                    (ScalarType::Float, PrismaValue::Float(f)) => PrismaValue::Float(f),
                    (ScalarType::Float, PrismaValue::Int(i)) => PrismaValue::Float(i as f64),

                    (ScalarType::Enum(ref et), PrismaValue::Enum(ref ev)) => match et.value_for(&ev.name) {
                        Some(_) => PrismaValue::Enum(ev.clone()),
                        None => {
                            return Err(CoreError::SerializationError(format!(
                                "Enum value '{}' not found on enum '{}'",
                                ev.as_string(),
                                et.name
                            )))
                        }
                    },

                    (ScalarType::Boolean, PrismaValue::Boolean(b)) => PrismaValue::Boolean(b),
                    (ScalarType::DateTime, PrismaValue::DateTime(dt)) => PrismaValue::DateTime(dt),
                    (ScalarType::Json, PrismaValue::Json(j)) => PrismaValue::Json(j),
                    (ScalarType::UUID, PrismaValue::Uuid(u)) => PrismaValue::Uuid(u),

                    (st, pv) => {
                        return Err(CoreError::SerializationError(format!(
                            "Attempted to serialize scalar '{}' with incompatible type '{:?}'",
                            pv, st
                        )))
                    }
                };

                Ok(Item::Value(item_value))
            }
            (pv, ot) => {
                return Err(CoreError::SerializationError(format!(
                    "Attempted to serialize scalar '{}' with non-scalar compatible type '{:?}'",
                    pv, ot
                )))
            }
        }
    }

    // fn serialize_enum(result: ReadQueryResult, typ: EnumTypeRef) -> CoreResult<PrismaValue> {
    //     unimplemented!()
    // }
}

// /// Attempts to coerce the given write result into the provided output type.
// fn coerce_result(result: WriteQueryResult, typ: &OutputTypeRef) -> CoreResult<QueryResult> {
//     let value: PrismaValue = match result.identifier {
//         Identifier::Id(id) => id.into(),
//         Identifier::Count(c) => PrismaValue::from(c), // Requires object with one field that is usize / int / float, or single scalar type.
//         Identifier::Record(r) => unimplemented!(),    // Requires object. Try coercing all fields of the object.
//         Identifier::None => unimplemented!(),         // Null?
//     };

//     unimplemented!()
// }

// fn coerce_value_type(val: PrismaValue, typ: &OutputTypeRef) -> CoreResult<()> {
//     match typ.borrow() {
//         OutputType::Object(o) => unimplemented!(),
//         OutputType::Opt(inner) => unimplemented!(),
//         OutputType::Enum(e) => unimplemented!(),
//         OutputType::List(inner) => unimplemented!(),
//         OutputType::Scalar(s) => unimplemented!(),
//     };

//     unimplemented!()
// }

// fn coerce_scalar() -> CoreResult<()> {
//     unimplemented!()
// }

/// Removes the excess records added to by the database query layer based on the query arguments
/// This would be the right place to add pagination markers (has next page, etc.).
pub fn trim_records(data: &mut Vec<Item>, query_args: &QueryArguments) {
    // The query engine reverses lists when querying for `last`, so we need to reverse again to have the intended order.
    if query_args.last.is_some() {
        data.reverse();
    }

    match (query_args.first, query_args.last) {
        (Some(f), _) if data.len() > f as usize => drop_right(data, 1),
        (_, Some(l)) if data.len() > l as usize => drop_left(data, 1),
        _ => (),
    };
}

/// Drops x records on the end of the wrapped records in place.
fn drop_right<T>(vec: &mut Vec<T>, x: u32) {
    vec.truncate(vec.len() - x as usize);
}

/// Drops x records on the start of the wrapped records in place.
fn drop_left<T>(vec: &mut Vec<T>, x: u32) {
    vec.reverse();
    drop_right(vec, x);
    vec.reverse();
}
