//! Prisma Response (Intermediate Data Representation)
//!
//! This module takes care of processing the results
//! and transforming them into a different AST.
//!
//! This IR is meant for general processing and storage.
//! It can also be easily serialized.

// mod lists;
// mod maps;

use crate::{
    CoreError, CoreResult, EnumTypeRef, IntoArc, ObjectTypeStrongRef, OutputType, OutputTypeRef, ResultPair, ScalarType,
};
use connector::{QueryArguments, QueryResult, ReadQueryResult, WriteQueryResult};
use indexmap::IndexMap;
use prisma_models::{GraphqlId, PrismaValue};
use std::{borrow::Borrow, collections::HashMap, convert::TryFrom, sync::Arc};

type ItemsWithParents = HashMap<Option<GraphqlId>, Vec<Item>>;

/// A `key -> value` map to an IR item
pub type Map = IndexMap<String, Item>;

/// A list of IR items
pub type List = Vec<Item>;

/// A response can either be some `key-value` data representation
/// or an error that occured.
#[derive(Debug)]
pub enum Response {
    Data(String, Item),
    Error(String),
}

/// An IR item that either expands to a subtype or leaf-record.
#[derive(Debug)]
pub enum Item {
    Map(Map),
    List(List),
    Value(PrismaValue),
}

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
                        let serialized = Self::serialize_read(r, &typ, None);

                        match serialized {
                            Ok(result) => {
                                // On the top level, each result pair boils down to a exactly a single serialized result.
                                // All checks for lists and optionals have already been performed during the recursion,
                                // so we just unpack the only result.
                                let (_, mut item_vec) = result.into_iter().take(1).next().unwrap();
                                vec.push(Response::Data(name, item_vec.pop().unwrap()))
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
    /// The enclosing type is required to make non-local decisions like if something is a list or not.
    ///
    /// Returns a pair of (parent ID, response)
    fn serialize_read(
        result: ReadQueryResult,
        typ: &OutputTypeRef,
        enclosing_type: Option<&OutputTypeRef>,
    ) -> CoreResult<ItemsWithParents> {
        // For each parent / items pair check the enclosing type constraint
        let result: ItemsWithParents = match typ.borrow() {
            OutputType::Object(obj) => Self::serialize_objects(result, obj.into_arc())?,
            OutputType::List(inner) => Self::serialize_read(result, inner, Some(typ))?,
            OutputType::Opt(inner) => Self::serialize_read(result, inner, Some(typ))?,
            _ => unreachable!(), // We always serialize reads into objects or lists.
                                 // OutputType::Enum(et) => unimplemented!(),
                                 // OutputType::Scalar(st) => unimplemented!(),
        };

        //

        // Based on the enclosing type, check / coerce the results:
        // Go through the result map and for each parent node, check the shape of the dependent results.
        // - If it's a list, trim records based on query args
        // - If it's an opt, check whatever is contained for nulls.
        let result: ItemsWithParents = if let Some(enclosing_type) = enclosing_type {
            match enclosing_type.borrow() {
                OutputType::List(_) => unimplemented!(), // keep the list and trim based on query args?
                OutputType::Opt(_) => result
                    .into_iter()
                    .map(|(parent, nodes)| {
                        if nodes.is_empty() {
                            (parent, vec![Item::Value(PrismaValue::Null)])
                        } else {
                            (parent, nodes)
                        }
                    })
                    .collect(),
                _ => unreachable!(),
            }
        } else {
            unimplemented!()
        };

        // - Else it's a single node per parent?

        Ok(result)

        // The optional handling above makes sure that if a field is nullable, it's returned as PrismaValue::Null.
        // match item {
        //     Some(item) => (parent, Response::Data(name, item)),
        //     None => Err(CoreError::SerializationError(format!("Required field {} returned null.", name))),
        // }
    }

    /// Serializes the given result into objects of given type.
    /// Makes no assumption about the arity of the result set.
    /// Returns a vector of serialized objects (as Item::Map), grouped into a map by parent, if present.
    fn serialize_objects(mut result: ReadQueryResult, typ: ObjectTypeStrongRef) -> CoreResult<ItemsWithParents> {
        // The way our query execution works, we only need to look at nested + lists if we hit an object.
        // Move lists and nested out of result for separate processing.
        let nested = std::mem::replace(&mut result.nested, vec![]);
        let lists = std::mem::replace(&mut result.lists, vec![]);

        // For each nested selected field we need to map the parents to their items.
        // { nested field -> { parent -> items } }
        let mut nested_mapping: HashMap<String, ItemsWithParents> = HashMap::new();

        // Parse and validate all nested objects with their respective output type.
        // Unwraps are safe due to query validation.
        for nested_result in nested {
            let name = nested_result.name.clone();
            let field = typ.find_field(&name).unwrap();
            let result = Self::serialize_read(nested_result, &field.field_type, None)?;

            // Check shape?

            nested_mapping.insert(name, result);
        }

        // For each selected list field we need to map the parents to their items.
        // { list field -> { parent -> items } }
        let mut list_mapping: HashMap<String, ItemsWithParents> = HashMap::new();
        for list_result in lists {
            let field = typ.find_field(&list_result.0).unwrap();

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

            list_mapping.insert(field.name.clone(), ItemsWithParents::new());

            for list_pair in list_result.1 {
                let converted: Vec<Item> = list_pair
                    .values
                    .into_iter()
                    .map(|val| Self::serialize_scalar(val, &list_type))
                    .collect::<CoreResult<Vec<_>>>()?;

                list_mapping
                    .get_mut(&field.name)
                    .unwrap()
                    .insert(Some(list_pair.record_id), converted);
            }
        }

        // Finally, serialize the objects based on the selected fields.
        let mut object_mapping = ItemsWithParents::new();
        let scalar_field_names = result.scalars.field_names; // Field names as in the ManyRecords

        // Write all fields, nested and list fields unordered into a map, afterwards order all into the final order.
        for record in result.scalars.records {
            let record_id = Some(record.collect_id(&scalar_field_names, &result.id_field)?);

            if !object_mapping.contains_key(&record.parent_id) {
                object_mapping.insert(record.parent_id.clone(), vec![]);
            }

            let mut object: HashMap<String, Item> = HashMap::new();

            // Write scalars
            let values = record.values;
            for (val, field_name) in values.into_iter().zip(scalar_field_names.iter()) {
                let field = typ.find_field(field_name).unwrap();
                object.insert(field_name.to_owned(), Self::serialize_scalar(val, &field.field_type)?);
            }

            // Write nested & lists
            // Check arity here?
            nested_mapping.iter_mut().for_each(|(field_name, mut inner)| {
                let val = inner.remove(&record_id).unwrap();
                object.insert(field_name.to_owned(), Item::List(val));
            });

            // Reorder into final form
            let mut map = Map::new();

            result.fields.iter().for_each(|field_name| {
                map.insert(field_name.to_owned(), object.remove(field_name).unwrap());
            });

            object_mapping.get_mut(&record.parent_id).unwrap().push(Item::Map(map));
        }

        Ok(object_mapping)
    }

    fn serialize_scalar(value: PrismaValue, typ: &OutputTypeRef) -> CoreResult<Item> {
        let item_value = match (&value, typ.borrow()) {
            (PrismaValue::Null, OutputType::Opt(_)) => PrismaValue::Null,
            (_, OutputType::Scalar(st)) => match (st, value) {
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
            },
            (pv, ot) => {
                return Err(CoreError::SerializationError(format!(
                    "Attempted to serialize scalar '{}' with non-scalar compatible type '{:?}'",
                    pv, ot
                )))
            }
        };

        Ok(Item::Value(item_value))
    }

    fn serialize_enum(result: ReadQueryResult, typ: EnumTypeRef) -> CoreResult<PrismaValue> {
        unimplemented!()
    }
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
