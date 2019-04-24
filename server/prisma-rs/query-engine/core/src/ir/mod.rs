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

mod lists;
mod maps;
mod utils;

use crate::ReadQueryResult;
use indexmap::IndexMap;
use prisma_models::GraphqlId;
use prisma_models::PrismaValue;

/// A response set maps to a Vec<PrismaQueryResponse>
/// where each represents the result of a query
pub type ResponseSet = Vec<Response>;

/// A response can either be some `key-value` data representation
/// or an error that occured.
pub enum Response {
    /// A data item has a name it will be returned under, and and actual item.
    Data(String, Item),
    Error(String), // FIXME: Use actual error type
}

/// A `key -> value` map to an IR item
pub type Map = IndexMap<String, Item>;

/// A list of IR items
pub type List = Vec<Item>;

/// An IR item that either expands to a subtype or leaf-record
#[derive(Debug)]
pub enum Item {
    /// (Parent ID, transformed record as map)
    Map(Option<GraphqlId>, Map),
    List(List),
    Value(PrismaValue),
}

/// A serialization IR builder utility
pub struct Builder(Vec<ReadQueryResult>);

impl Builder {
    pub fn new() -> Self {
        Self(vec![])
    }

    /// Add a single query result to the builder
    pub fn add(mut self, q: ReadQueryResult) -> Self {
        self.0.push(q);
        self
    }

    /// Parse collected queries into the return wrapper type
    pub fn build(self) -> ResponseSet {
        self.0.into_iter().fold(vec![], |mut vec, res| {
            vec.push(match res {
                ReadQueryResult::Single(query) => Response::Data(query.name.clone(), Item::Map(maps::build_map(query))),
                ReadQueryResult::Many(query) => {
                    Response::Data(query.name.clone(), Item::List(lists::build_list(query)))
                }
            });
            vec
        })
    }
}
