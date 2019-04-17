//! Utilities required for the data intermediate representation

use super::{Item, List, Map};
use connector::ScalarListValues;
use prisma_models::GraphqlId;

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

// pub fn fold_list_result(list_results: Vec<(String, Vec<ScalarListValues>)>) -> ListValues {
//     let field_names: Vec<_> = list_results.iter().map(|(a, _)| a.clone()).collect();

//     let values: Vec<Vec<Vec<_>>> = list_results
//         .into_iter()
//         .map(|(_, vec)| vec.into_iter().map(|s| s.values).collect())
//         .collect();

//     ListValues { field_names, values }
// }
