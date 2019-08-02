//! Prisma Response IR (Intermediate Representation).
//!
//! This module takes care of processing the results
//! and transforming them into a different AST.
//!
//! This IR is meant for general processing and storage.
//! It can also be easily serialized.
//!
//! The code itself can be considered WIP. It is clear when reading the code that there are missing abstractions
//! and a restructure might be necessary (good example is the default value handling sprinkled all over the place).
mod read;
mod utils;
mod write;

pub use read::*;
pub use utils::*;
pub use write::*;

use crate::{OutputType, ResultPair};
use indexmap::IndexMap;
use prisma_models::PrismaValue;
use std::{borrow::Borrow, sync::Arc};

/// A `key -> value` map to an IR item
pub type Map = IndexMap<String, Item>;

/// A list of IR items
pub type List = Vec<Item>;

/// Convenience type wrapper for Arc<Item>.
pub type ItemRef = Arc<Item>;

/// A response can either be some `key-value` data representation
/// or an error that occured.
#[derive(Debug)]
pub enum Response {
    Data(String, Item),
    Error(String),
}

/// An IR item that either expands to a subtype or leaf-record.
#[derive(Debug, Clone)]
pub enum Item {
    Map(Map),
    List(List),
    Value(PrismaValue),

    /// Wrapper type to allow multiple parent records
    /// to claim the same item without copying data
    /// (serialization can then choose how to copy if necessary).
    Ref(ItemRef),
}

/// An IR builder utility
#[derive(Debug)]
pub struct ResultIrBuilder(Vec<ResultPair>);

impl ResultIrBuilder {
    pub fn new() -> Self {
        Self(vec![])
    }

    /// Add a single query result to the builder
    pub fn add(mut self, q: ResultPair) -> Self {
        self.0.push(q);
        self
    }

    /// Parse collected queries into the return wrapper type
    pub fn build(self) -> Vec<Response> {
        self.0
            .into_iter()
            .fold(vec![], |mut vec, res| {
                match res {
                    ResultPair::Read(r, typ) => {
                        let name = r.alias.clone().unwrap_or_else(|| r.name.clone());
                        let serialized = serialize_read(r, &typ, false, false);

                        match serialized {
                            Ok(result) => {
                                // On the top level, each result pair boils down to a exactly a single serialized result.
                                // All checks for lists and optionals have already been performed during the recursion,
                                // so we just unpack the only result possible.
                                let result = if result.is_empty() {
                                    match typ.borrow() {
                                        OutputType::Opt(_) => Item::Value(PrismaValue::Null),
                                        OutputType::List(_) => Item::List(vec![]),
                                        _ => unreachable!(),
                                    }
                                } else {
                                    let (_, item) = result.into_iter().take(1).next().unwrap();
                                    item
                                };

                                vec.push(Response::Data(name, result));
                            }

                            Err(err) => vec.push(Response::Error(format!("{}", err))),
                        };
                    }
                    _ => unimplemented!(),
                };

                vec
            })
            .into_iter()
            .collect()
    }
}
