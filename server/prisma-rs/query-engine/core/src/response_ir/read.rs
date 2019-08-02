use super::*;
use crate::{CoreError, CoreResult, IntoArc, ObjectTypeStrongRef, OutputType, OutputTypeRef, ScalarType};
use connector::{ReadQueryResult, ScalarListValues};
use indexmap::IndexMap;
use prisma_models::{GraphqlId, PrismaValue};
use std::{borrow::Borrow, collections::HashMap, convert::TryFrom};

/// A grouping of items to their parent record.
/// The item implicitly holds the information of the type of item contained.
/// E.g., if the output type of a field designates a single object, the item will be
/// Item::Map(map), if it's a list, Item::List(list), etc. (hence "checked")
type CheckedItemsWithParents = IndexMap<Option<GraphqlId>, Item>;

/// A grouping of items to their parent record.
/// As opposed to the checked mapping, this map isn't holding final information about
/// the contained items, i.e. the Items are all unchecked.
type UncheckedItemsWithParents = IndexMap<Option<GraphqlId>, Vec<Item>>;

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
pub fn serialize_read(
    result: ReadQueryResult,
    typ: &OutputTypeRef,
    is_list: bool,
    is_optional: bool,
) -> CoreResult<CheckedItemsWithParents> {
    let query_args = result.query_arguments.clone();
    let name = result.name.clone();

    match typ.borrow() {
        OutputType::List(inner) => serialize_read(result, inner, true, false),
        OutputType::Opt(inner) => serialize_read(result, inner, is_list, true),
        OutputType::Object(obj) => {
            let result = serialize_objects(result, obj.into_arc())?;

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
fn serialize_objects(mut result: ReadQueryResult, typ: ObjectTypeStrongRef) -> CoreResult<UncheckedItemsWithParents> {
    // The way our query execution works, we only need to look at nested + lists if we hit an object.
    // Move lists and nested out of result for separate processing.
    let nested = std::mem::replace(&mut result.nested, vec![]);
    let lists = std::mem::replace(&mut result.lists, vec![]);

    // { <nested field name> -> { parent ID -> items } }
    let mut nested_mapping: HashMap<String, CheckedItemsWithParents> = process_nested_results(nested, &typ)?;

    // We need the Arcs to solve the issue where we have multiple parents claiming the same data (we want to move the data out of the nested structure
    // to prevent expensive copying during serialization).

    // { <list field name> -> { parent ID -> items } }
    let mut list_mapping = process_scalar_lists(lists, &typ)?;

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
                object.insert(field_name.to_owned(), serialize_scalar(val, &field.field_type)?);
            }
        }

        // Write nested results & lists
        write_nested_items(&record_id, &mut nested_mapping, &mut object, &typ);
        write_nested_items(&record_id, &mut list_mapping, &mut object, &typ);

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

/// Unwraps are safe due to query validation.
fn write_nested_items(
    record_id: &Option<GraphqlId>,
    items_with_parent: &mut HashMap<String, CheckedItemsWithParents>,
    into: &mut HashMap<String, Item>,
    enclosing_type: &ObjectTypeStrongRef,
) {
    items_with_parent.iter_mut().for_each(|(field_name, inner)| {
        let val = inner.get(record_id);

        // The value must be a reference (or None - handle default), everything else is an error in the serialization logic.
        match val {
            Some(Item::Ref(ref r)) => {
                into.insert(field_name.to_owned(), Item::Ref(ItemRef::clone(r)));
            }

            None => {
                let field = enclosing_type.find_field(field_name).unwrap();
                let default = match field.field_type.borrow() {
                    OutputType::List(_) => Item::List(vec![]),
                    OutputType::Opt(inner) => {
                        if inner.is_list() {
                            Item::List(vec![])
                        } else {
                            Item::Value(PrismaValue::Null)
                        }
                    }
                    _ => unreachable!(),
                };

                into.insert(field_name.to_owned(), Item::Ref(ItemRef::new(default)));
            }
            _ => panic!("Application logic invariant error: Nested items have to be wrapped as a Item::Ref."),
        };
    });
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
        let result = serialize_read(nested_result, &field.field_type, false, false)?;

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
                .map(|val| serialize_scalar(val, &list_type))
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
        (_, OutputType::Opt(inner)) => serialize_scalar(value, inner),
        (_, OutputType::Enum(et)) => match value {
            PrismaValue::String(s) => match et.value_for(&s) {
                Some(ev) => Ok(Item::Value(PrismaValue::Enum(ev.clone()))),
                None => Err(CoreError::SerializationError(format!(
                    "Value '{}' not found in enum '{:?}'",
                    s, et
                ))),
            },

            PrismaValue::Enum(ref ev) => match et.value_for(&ev.name) {
                Some(_) => Ok(Item::Value(PrismaValue::Enum(ev.clone()))),
                None => Err(CoreError::SerializationError(format!(
                    "Enum value '{}' not found on enum '{}'",
                    ev.as_string(),
                    et.name
                ))),
            },

            val => Err(CoreError::SerializationError(format!(
                "Attempted to serialize non-enum-compatible value '{}' with enum '{:?}'",
                val, et
            ))),
        },
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
