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

#[allow(dead_code)]
impl Envelope {
    pub fn convert(self) -> serde_json::Value {
        serde_json::to_value(&self).unwrap()
    }

    fn from_str(s: &str) -> Self {
        serde_json::from_str(s).unwrap()
    }
}
