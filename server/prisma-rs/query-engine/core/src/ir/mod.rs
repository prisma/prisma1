//! Prisma Intermediate Data Representation
//!
//! The data format returned via `PrismaQueryResult` isn't
//! very convenient to work with. This module takes care of
//! processing the results and transforming them into a
//! different AST
//!
//! This IR (intermediate representation) is meant for general
//! processing and storage. It can also be easily serialised.

mod lists;
mod maps;

use crate::ReadQueryResult;
use connector::QueryArguments;
use indexmap::IndexMap;
use prisma_models::GraphqlId;
use prisma_models::PrismaValue;

/// A response set maps to a Vec<PrismaQueryResponse>
/// where each represents the result of a query
pub type ResponseSet = Vec<Response>;

/// A response can either be some `key-value` data representation
/// or an error that occured.
#[derive(Debug)]
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
#[derive(Debug)]
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
                ReadQueryResult::Single(query) => {
                    let query_name = query.name.clone();
                    match maps::build_map(query) {
                        Some(m) => Response::Data(query_name, Item::Map(None, m)),
                        None => Response::Data(query_name, Item::Value(PrismaValue::Null)),
                    }
                }
                ReadQueryResult::Many(query) => {
                    let query_name = query.name.clone();
                    let query_args = query.query_arguments.clone();
                    let mut result = lists::build_list(query);

                    // Trim excess data from the processed result set
                    remove_excess_records(&mut result, &query_args);
                    Response::Data(query_name, Item::List(result))
                }
            });
            vec
        })
    }
}


/// Removes the excess records added to by the database query layer based on the query arguments
/// This would be the right place to add pagination markers (has next page, etc.).
pub fn remove_excess_records(data: &mut Vec<Item>, query_args: &QueryArguments) {
    // The query engine reverses lists when querying for `last`, so we need to reverse again to have the intended order.
    let reversed = query_args.last.is_some();
    if reversed {
        data.reverse();
    }

    match (query_args.first, query_args.last) {
        (Some(f), _) if data.len() > f as usize => drop_right(data, 1),
        (_, Some(l)) if data.len() > l as usize => drop_left(data, 1),
        _ => (),
    };
}


/// Drops x records on the end of the wrapped records in place.
fn drop_right<T>(vec: &mut Vec<T>, x: u32) {
    vec.truncate(vec.len() - x as usize);
}

/// Drops x records on the start of the wrapped records in place.
fn drop_left<T>(vec: &mut Vec<T>, x: u32) {
    vec.reverse();
    drop_right(vec, x);
    vec.reverse();
}