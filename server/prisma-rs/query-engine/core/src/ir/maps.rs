//! Process a record into an IR Map

use crate::{PrismaQueryResult, SinglePrismaQueryResult};
use super::{Map, Item, lists::build_list};

pub fn build_map(result: &SinglePrismaQueryResult) -> Map {
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
            PrismaQueryResult::Single(nested) => map.insert(nested.name.clone(), Item::Map(build_map(nested))),
            PrismaQueryResult::Multi(nested) => map.insert(nested.name.clone(), Item::List(build_list(nested))),
        };

        map
    });

    result
        .list_results
        .values
        .iter()
        .zip(&result.list_results.field_names)
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