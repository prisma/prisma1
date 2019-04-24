//! Process a set of records into an IR List

use super::{maps::build_map, utils, Item, List, Map};
use crate::{ManyReadQueryResults, ReadQueryResult};
use prisma_models::{GraphqlId, PrismaValue};
use std::collections::HashMap;

enum ParentsWithRecords {
    Single(HashMap<GraphqlId, Vec<Item>>),
    Many(HashMap<GraphqlId, Vec<Item>>),
}

impl ParentsWithRecords {
    pub fn contains_key(&self, key: &GraphqlId) -> bool {
        match self {
            ParentsWithRecords::Single(m) => m.contains_key(key),
            ParentsWithRecords::Many(m) => m.contains_key(key),
        }
    }

    pub fn insert(&mut self, key: GraphqlId, value: Vec<Item>) {
        match self {
            ParentsWithRecords::Single(m) => m.insert(key, value),
            ParentsWithRecords::Many(m) => m.insert(key, value),
        };
    }

    pub fn get_mut(&mut self, key: &GraphqlId) -> Option<&mut Vec<Item>> {
        match self {
            ParentsWithRecords::Single(m) => m.get_mut(key),
            ParentsWithRecords::Many(m) => m.get_mut(key),
        }
    }

    pub fn get(&self, key: &GraphqlId) -> Option<&Vec<Item>> {
        match self {
            ParentsWithRecords::Single(m) => m.get(key),
            ParentsWithRecords::Many(m) => m.get(key),
        }
    }

    pub fn remove(&mut self, key: &GraphqlId) -> Option<Vec<Item>> {
        match self {
            ParentsWithRecords::Single(m) => m.remove(key),
            ParentsWithRecords::Many(m) => m.remove(key),
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

    // todo: this might have issues with empty results.

    // Group nested results by parent ids and move them into the grouped map.
    nested.into_iter().for_each(|nested_result| {
        match nested_result {
            ReadQueryResult::Single(single) => unimplemented!(),
            ReadQueryResult::Many(many) => {
                if !nested_fields_to_groups.contains_key(&many.name) {
                    nested_fields_to_groups.insert(many.name.clone(), ParentsWithRecords::Many(HashMap::new()));
                }

                let parents_with_records = nested_fields_to_groups.get_mut(&many.name).unwrap();
                let nested_build = build_list(many);
                nested_build.into_iter().for_each(|item| match item {
                    Item::Map(parentOpt, i) => {
                        // unwrap is safe because we know that we have to have a parent for nested maps.
                        let parent_id = parentOpt.expect("Expected parent ID to be present on nested query results.");
                        if parents_with_records.contains_key(&parent_id) {
                            parents_with_records.insert(parent_id, vec![]);
                        }

                        let records_for_parent = parents_with_records.get_mut(&parent_id).unwrap();
                        records_for_parent.push(Item::Map(parentOpt, i));
                    }
                    _ => unreachable!(),
                });
            }
        }
    });

    // { scalar list field name -> { record id -> values } }
    let mut lists_to_groups: HashMap<String, HashMap<String, Vec<PrismaValue>>> = HashMap::new();

    lists.into_iter().for_each(|(list_field, list_values)| {
        lists_to_groups.insert(&list_field, HashMap::new());
        let map = lists_to_groups.get(&list_field)
    //         pub node_id: GraphqlId,
    // pub values: Vec<PrismaValue>,
            list_values.into_iter().for_each(|value| {

                value.node_id
            });
        ));
    });

    // There is always at least one scalar selected (id), making scalars the perfect entry point.
    result
        .scalars
        .nodes
        .into_iter()
        .map(|record| {
            let record_id = record
                .get_id_value(&field_names, result.selected_fields.model())
                .expect("Expected ID value to be present in the result set for each returned record.");

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
            nested.iter().for_each(|n| {
                let field_name = n.name();

                // Unwraps are safe due to the preprocessing done above.
                match nested_fields_to_groups.get(&field_name).unwrap() {
                    ParentsWithRecords::Single(m) => {
                        let records = m.remove(&record_id).unwrap_or(vec![Item::Value(PrismaValue::Null)]);
                        base_map.insert(field_name.clone(), records.pop().unwrap());
                    }
                    ParentsWithRecords::Many(m) => {
                        let records = m.remove(&record_id).unwrap_or(vec![]);
                        base_map.insert(field_name.clone(), Item::List(records));
                    }
                }
            });

            Item::Map(record.parent_id, base_map)
        })
        .collect();

    // // Explicitly handle scalar-list results
    // let ids = result.find_ids().expect("Failed to find record IDs!");
    // let scalar_values = utils::associate_list_results(ids, &result.lists);

    // // Then just merge the maps into the existing data
    // vec = vec.into_iter().zip(scalar_values).fold(vec![], |mut vec, iter| {
    //     vec.push(Item::Map(match iter {
    //         (Item::Map(map), Item::Map(scalars)) => scalars.into_iter().fold(map, |mut map, (k, v)| {
    //             map.insert(k, v);
    //             map
    //         }),
    //         _ => unreachable!("Tried merging two `Item`s that were not `Map`"),
    //     }));

    //     vec
    // });

    // // Re-order fields to be in-line with what the query specified
    // // This also removes implicitly selected fields (like IDs).
    // vec.into_iter()
    //     .fold(vec![], |mut vec, mut item| {
    //         if let Item::Map(ref mut map) = item {
    //             vec.push(result.fields.iter().fold(Map::new(), |mut new, field| {
    //                 let item = map.remove(field).expect("[List]: Missing required field");
    //                 new.insert(field.clone(), item);
    //                 new
    //             }));
    //         }

    //         vec
    //     })
    //     .into_iter()
    //     .map(|i| Item::Map(i))
    //     .collect()

    unimplemented!()
}
