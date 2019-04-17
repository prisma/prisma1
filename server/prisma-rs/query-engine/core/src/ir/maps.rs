//! Process a record into an IR Map

use super::{lists::build_list, utils, Item, Map};
use crate::{ReadQueryResult, SingleReadQueryResult};

pub fn build_map(result: &SingleReadQueryResult) -> Map {
    let ids = result.find_id().expect("Failed to find record IDs!");
    let scalar_values = utils::associate_list_results(vec![ids], &result.list_results);
    let mut outer = Map::new();

    scalar_values.into_iter().for_each(|item| {
        if let Item::Map(map) = item {
            map.into_iter().for_each(|(k, v)| {
                outer.insert(k, v);
            });
        }
    });

    // Build selected fields first
    let mut outer = match &result.result {
        Some(single) => single
            .field_names
            .iter()
            .zip(&single.node.values)
            .fold(Map::new(), |mut map, (name, val)| {
                map.insert(name.clone(), Item::Value(val.clone()));
                map
            }),
        None => panic!("No result found"), // FIXME: Can this ever happen?
    };

    // Then add nested selected fields
    outer = result.nested.iter().fold(outer, |mut map, query| {
        match query {
            ReadQueryResult::Single(nested) => map.insert(nested.name.clone(), Item::Map(build_map(nested))),
            ReadQueryResult::Many(nested) => map.insert(nested.name.clone(), Item::List(build_list(nested))),
        };

        map
    });


    result.fields.iter().fold(Map::new(), |mut map, field| {
        map.insert(
            field.clone(),
            outer.remove(field).expect("[Map]: Missing required field"),
        );
        map
    })
}
