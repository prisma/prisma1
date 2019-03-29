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
pub type List = Vec<Item>;

#[derive(Serialize, Deserialize)]
pub enum Item {
    Map(Map),
    List(List),
    Value(PrismaValue),
}

use PrismaQueryResult::*;

#[allow(dead_code)]
impl Envelope {
    pub fn convert(self) -> serde_json::Value {
        serde_json::to_value(&self).unwrap()
    }

    fn from_str(s: &str) -> Self {
        serde_json::from_str(s).unwrap()
    }
}

#[allow(dead_code)]
pub fn build(result: &PrismaQueryResult) -> Envelope {
    match result {
        Single(result) => build_map(result).into(),
        Multi(result) => build_list(result).into(),
    }
}

#[allow(dead_code)]
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
    result
        .result
        .as_pairs()
        .iter()
        .map(|vec| {
            Item::Map(vec.iter().fold(Map::new(), |mut map, (name, value)| {
                map.insert(name.clone(), Item::Value(value.clone()));
                map
            }))
        })
        .collect()
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
