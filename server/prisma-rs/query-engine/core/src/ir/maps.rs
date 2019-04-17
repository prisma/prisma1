//! Process a record into an IR Map

use super::{lists::build_list, Item, Map};
use crate::{ReadQueryResult, SingleReadQueryResult};

pub fn build_map(result: &SingleReadQueryResult) -> Map {
    // Build selected fields first
    let mut outer = match &result.scalars {
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

    result
        .lists
        .values
        .iter()
        .zip(&result.lists.field_names)
        .for_each(|(values, field_name)| {
            outer.insert(
                field_name.clone(),
                Item::List(values.iter().fold(vec![], |_, list| {
                    list.iter().map(|pv| Item::Value(pv.clone())).collect()
                })),
            );
        });

    result.fields.iter().fold(Map::new(), |mut map, field| {
        map.insert(
            field.clone(),
            outer.remove(field).expect("[Map]: Missing required field"),
        );
        map
    })
}
