//! Utilities required for the data intermediate representation

use super::{Item, List, Map};
use connector::ScalarListValues;
use prisma_models::{GraphqlId, SelectedScalarField};

/// This function transforms list results into a presentation that eases the mapping of list results
/// to their corresponding records on higher layers.
///
/// ```
/// [ // all records
///     [ // one record
///         [ List A ], // one list
///         [ List B ],
///     ],
///     [ // one record
///         [ List A ], // one list
///         [ List B ],
///     ],
///     [ // one record
///         [ List A ], // one list
///         [ List B ],
///     ]
/// ]
/// ```
///
pub fn associate_list_results(ids: Vec<&GraphqlId>, list_results: &Vec<(String, Vec<ScalarListValues>)>) -> List {
    ids.iter().fold(vec![], |mut map, id| {
        let field_names: Vec<_> = list_results.iter().map(|(a, _)| a.clone()).collect();

        // Create an empty list for every field on every node
        let mut record = field_names.iter().fold(Map::new(), |mut map, name| {
            map.insert(name.clone(), Item::List(vec![]));
            map
        });

        // Then write actual scalar list data into tempalte
        list_results.iter().for_each(|(field_name, vec)| {
            vec.iter().for_each(|s| {
                if &s.node_id == *id {
                    record.insert(
                        field_name.clone(),
                        Item::List(s.values.iter().map(|pv| Item::Value(pv.clone())).collect()),
                    );
                }
            })
        });

        map.push(Item::Map(record));
        map
    })
}

/// Given a collection, it removes all implicitly added fields from it again
pub fn remove_implicit_fields(implicits: &Vec<&SelectedScalarField>, data: Map) -> Map {
    // Iterate through the given map
    data.into_iter()
        .filter_map(|(key, value)| {
            match value {
                // If we have a map, we strip all implicit keys and recurse
                Item::Map(map) => Some((
                    key,
                    Item::Map(remove_implicit_fields(
                        implicits,
                        map.into_iter()
                            .filter_map(|(key2, val)| {
                                implicits
                                    .iter()
                                    .find(|sf| sf.field.name == key2)
                                    .map_or_else(|| Some((key2, val)), |_| None)
                            })
                            .collect(),
                    )),
                )),
                // For a list we only check for maps to recurse into
                Item::List(list) => Some((
                    key,
                    Item::List(
                        list.into_iter()
                            .map(|item| match item {
                                Item::Map(map) => Item::Map(remove_implicit_fields(implicits, map)),
                                ignore => ignore,
                            })
                            .collect(),
                    ),
                )),
                // All other keys are ignored
                ignore => Some((key, ignore)),
            }
        })
        // Fold back up to the correct map
        .fold(Map::new(), |mut map, (k, v)| {
            map.insert(k, v);
            map
        })
}
