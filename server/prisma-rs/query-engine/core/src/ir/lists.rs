//! Process a set of records into an IR List

use super::{maps::build_map, Item, List, Map, remove_excess_records};
use crate::{ManyReadQueryResults, ReadQueryResult};
use prisma_models::{GraphqlId, PrismaValue};
use std::{collections::{hash_map::IterMut, HashMap}, sync::Arc};

#[derive(Debug)]
enum ParentsWithRecords {
    Single(HashMap<GraphqlId, Item>),
    Many(HashMap<GraphqlId, Vec<Item>>),
}

impl ParentsWithRecords {
    pub fn iter_mut(&mut self) -> IterMut<GraphqlId, Vec<Item>> {
        match self {
            ParentsWithRecords::Single(_) => panic!("Can't call iter_mut on single parent with record"),
            ParentsWithRecords::Many(m) => m.iter_mut(),
        }
    }

    pub fn contains_key(&self, key: &GraphqlId) -> bool {
        match self {
            ParentsWithRecords::Single(m) => m.contains_key(key),
            ParentsWithRecords::Many(m) => m.contains_key(key),
        }
    }

    pub fn insert(&mut self, key: GraphqlId, mut value: Vec<Item>) {
        match self {
            ParentsWithRecords::Single(m) => {
                m.insert(
                    key,
                    value
                        .pop()
                        .expect("Expected to insert at least one item for single result."),
                );
            }
            ParentsWithRecords::Many(m) => {
                m.insert(key, value);
            }
        };
    }

    pub fn get_mut(&mut self, key: &GraphqlId) -> Option<&mut Vec<Item>> {
        match self {
            ParentsWithRecords::Single(_) => panic!("Can't call get_mut on single parent with record"),
            ParentsWithRecords::Many(m) => m.get_mut(key),
        }
    }
}

pub fn build_list(mut result: ManyReadQueryResults) -> List {
    let field_names = result.scalars.field_names;

    // First, move lists and nested out of result for separate processing.
    let nested = std::mem::replace(&mut result.nested, vec![]);
    let lists = std::mem::replace(&mut result.lists, vec![]);

    // { Queried relation field name -> { Parent record ID -> Vec<NestedRecords> } }
    // We need the ParentsWithRecords indirection to preserve information if the nesting is to-one or to-many.
    let mut nested_fields_to_groups: HashMap<String, ParentsWithRecords> = HashMap::new();

    // todo: The code below might have issues with empty results. To test.

    // Group nested results by parent ids and move them into the grouped map.
    nested.into_iter().for_each(|nested_result| match nested_result {
        ReadQueryResult::Single(single) => {
            if single.scalars.is_some() {
                let parent_id = single
                    .parent_id()
                    .cloned()
                    .expect("Parent ID needs to be present on nested results.");

                if !nested_fields_to_groups.contains_key(&single.name) {
                    nested_fields_to_groups.insert(single.name.clone(), ParentsWithRecords::Single(HashMap::new()));
                }

                let parents_with_records = nested_fields_to_groups
                    .get_mut(&single.name)
                    .expect("Parents with records mapping must contain entries for all nested queries.");;

                match build_map(single) {
                    Some(m) => parents_with_records.insert(parent_id.clone(), vec![Item::Map(Some(parent_id), m)]),
                    None => parents_with_records.insert(parent_id.clone(), vec![Item::Value(PrismaValue::Null)]),
                };

            }
        }
        ReadQueryResult::Many(many) => {
            if !nested_fields_to_groups.contains_key(&many.name) {
                nested_fields_to_groups.insert(many.name.clone(), ParentsWithRecords::Many(HashMap::new()));
            }

            let parents_with_records = nested_fields_to_groups
                .get_mut(&many.name)
                .expect("Parents with records mapping must contain entries for all nested queries.");

            let query_args = many.query_arguments.clone();
            let nested_build = build_list(many);

            nested_build.into_iter().for_each(|item| match item {
                Item::Map(parent_opt, i) => {
                    let parent_id = parent_opt
                        .clone()
                        .expect("Expected parent ID to be present on nested query results.");

                    if !parents_with_records.contains_key(&parent_id) {
                        parents_with_records.insert(parent_id.clone(), vec![]);
                    }

                    let records_for_parent = parents_with_records
                        .get_mut(&parent_id)
                        .expect("Expected records to parent mapping to contain entries for all nodes.");

                    records_for_parent.push(Item::Map(parent_opt, i));
                }
                _ => unreachable!(),
            });

            // Post process results for this query
            parents_with_records.iter_mut().for_each(|(_, v)| {
                remove_excess_records(v, &query_args);
            });
        }
    });

    // { scalar list field name -> { record id -> values } }
    let mut lists_to_groups: HashMap<String, HashMap<GraphqlId, Vec<PrismaValue>>> = HashMap::new();

    lists.into_iter().for_each(|(list_field, list_values)| {
        lists_to_groups.insert(list_field.clone(), HashMap::new());
        let map = lists_to_groups
            .get_mut(&list_field)
            .expect("Expected lists to groups to contain entries for all list fields.");

        list_values.into_iter().for_each(|value| {
            map.insert(value.node_id, value.values);
        });
    });

    let nested_field_names: Vec<String> = nested_fields_to_groups
        .keys()
        .clone()
        .into_iter()
        .map(|k| k.to_owned())
        .collect();

    let model = Arc::clone(&result.selected_fields.model());
    let final_field_order = result.fields.clone();

    // There is always at least one scalar selected (id), making scalars the perfect entry point.
    result
        .scalars
        .nodes
        .into_iter()
        .map(|record| {
            let record_id = record
                .get_id_value(&field_names, Arc::clone(&model))
                .expect("Expected ID value to be present in the result set for each returned record.")
                .clone();

            let mut base_map = Map::new();

            // Insert all scalars of given record into the base map.
            record
                .values
                .into_iter()
                .zip(field_names.iter())
                .for_each(|(value, name)| {
                    base_map.insert(name.clone(), Item::Value(value));
                });

            // For each nested query, find the relevant related records and insert them into the map.
            nested_field_names.iter().for_each(|field_name| {
                // let field_name = n.name();

                // Unwraps are safe due to the preprocessing done above.
                match nested_fields_to_groups
                    .get_mut(field_name)
                    .expect("Expected nested fields to groups map to be complete after preprocessing.")
                {
                    ParentsWithRecords::Single(m) => {
                        let record = m.remove(&record_id).unwrap_or(Item::Value(PrismaValue::Null));
                        base_map.insert(field_name.clone(), record);
                    }
                    ParentsWithRecords::Many(m) => {
                        let records = m.remove(&record_id).unwrap_or(vec![]);
                        base_map.insert(field_name.clone(), Item::List(records));
                    }
                }
            });

            // For each list, find the relevant nodes and insert them into the map.
            lists_to_groups.iter_mut().for_each(|(list_field_name, mapping)| {
                match mapping.remove(&record_id) {
                    Some(values) => base_map.insert(
                        list_field_name.clone(),
                        Item::List(values.into_iter().map(|v| Item::Value(v)).collect()),
                    ),
                    None => base_map.insert(list_field_name.clone(), Item::List(vec![])),
                };
            });

            // Reorder fields into final form.
            Item::Map(
                record.parent_id,
                final_field_order.iter().fold(Map::new(), |mut new, field| {
                    let item = base_map.remove(field).expect("Missing field for serialization.");
                    new.insert(field.clone(), item);
                    new
                }),
            )
        })
        .collect()
}
