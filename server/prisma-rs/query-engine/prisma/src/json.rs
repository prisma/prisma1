//! Json serialisation module

use core::{MultiPrismaQueryResult, PrismaQueryResult, SinglePrismaQueryResult};
use prisma_models::PrismaValue;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Wraps a response in a deterministic envelope
#[derive(Serialize, Deserialize)]
pub struct Envelope {
    tt: ResponseType,
    root: Item,
}

#[derive(Serialize, Deserialize)]
pub enum ResponseType {
    Data,
    Error,
}

pub type Map = HashMap<String, Item>;
pub type List = Vec<PrismaValue>;

#[derive(Serialize, Deserialize)]
pub enum Item {
    Map(Map),
    List(List),
    Value(PrismaValue),
}

use PrismaQueryResult::*;

///
pub fn build(result: PrismaQueryResult) -> Envelope {
    match result {
        Single(result) => build_map(&result).into(),
        Multi(result) => build_list(result).into(),
    }
}

fn build_map(result: &SinglePrismaQueryResult) -> Map {
    // Build selected fields first
    let outer = match &result.result {
        Some(single) => single
            .field_names
            .iter()
            .zip(&single.node.values)
            .fold(Map::new(), |mut map, (name, val)| {
                map.insert(name.clone(), Item::Value(val.clone()));
                map
            }),
        None => panic!("No result found"),
    };

    // Then add nested selected fields
    result.nested.iter().fold(outer, |mut map, query| {
        match query {
            Single(nested) => map.insert(nested.name.clone(), Item::Map(build_map(nested))),
            Multi(nested) => map.insert(nested.name.clone(), Item::List(build_list(nested))),
        };

        map
    })
}

fn build_list(result: &MultiPrismaQueryResult) -> List {
    // Ok(many_nodes
    //     .result
    //     .as_pairs()
    //     .into_iter()
    //     .map(|vec| {
    //         vec.into_iter()
    //             .fold(Ok(JsonMap::new()), |mut map: PrismaResult<JsonMap>, (name, value)| {

    //                 map.as_mut().unwrap().insert(name, serialize_prisma_value(&value)?);
    //                 map
    //             })
    //     })
    //     .map(|map| Value::Object(map.unwrap()))
    //     .collect())

    unimplemented!()
}

impl From<Map> for Envelope {
    fn from(map: Map) -> Self {
        Self {
            tt: ResponseType::Data,
            root: Item::Map(map),
        }
    }
}

impl From<List> for Envelope {
    fn from(list: List) -> Self {
        Self {
            tt: ResponseType::Data,
            root: Item::List(list),
        }
    }
}
