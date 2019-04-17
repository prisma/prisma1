//! Prisma Intermediate Data Representation
//!
//! The data format returned via `PrismaQueryResult` isn't
//! very convenient to work with. This module takes care of
//! processing the results and transforming them into a
//! different AST
//!
//! This IR (intermediate representation) is meant for general
//! processing and storage. It can also be easily serialised.
#![warn(warnings)]

mod maps;
mod lists;
mod utils;

use crate::PrismaQueryResult;
use indexmap::IndexMap;
use prisma_models::PrismaValue;

/// A response set maps to a Vec<PrismaQueraResponse>
/// where each represents the result of a query
pub type ResponseSet = Vec<Response>;

/// A response can either be some `key-value` data representation
/// or an error that occured.
pub enum Response {
    Data(String, Item),
    Error(String), // FIXME: Use actual error type
}

/// A `key -> value` map to an IR item
pub type Map = IndexMap<String, Item>;

/// A list of IR items
pub type List = Vec<Item>;

/// An IR item that either expands to a subtype or leaf-node
#[derive(Debug)]
pub enum Item {
    Map(Map),
    List(List),
    Value(PrismaValue),
}

/// A serialization IR builder utility
pub struct Builder(Vec<PrismaQueryResult>);

impl Builder {
    pub fn new() -> Self {
        Self(vec![])
    }

    /// Add a single query result to the builder
    pub fn add(mut self, q: PrismaQueryResult) -> Self {
        self.0.push(q);
        self
    }

    /// Parse collected queries into a wrapper type
    pub fn build(self) -> ResponseSet {
        self.0.into_iter().fold(vec![], |mut vec, res| {
            vec.push(match res {
                PrismaQueryResult::Single(query) => Response::Data(query.name.clone(), Item::Map(maps::build_map(&query))),
                PrismaQueryResult::Multi(query) => Response::Data(query.name.clone(), Item::List(lists::build_list(&query))),
            });
            vec
        })
    }
}
