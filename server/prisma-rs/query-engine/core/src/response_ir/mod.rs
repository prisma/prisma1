//! Prisma Response (Intermediate Data Representation)
//!
//! This module takes care of processing the results
//! and transforming them into a different AST.
//!
//! This IR is meant for general processing and storage.
//! It can also be easily serialized.

mod lists;
mod maps;

use crate::ResultPair;
use connector::{QueryArguments, ReadQueryResult};
use indexmap::IndexMap;
use prisma_models::{GraphqlId, PrismaValue};

/// A `key -> value` map to an IR item
pub type Map = IndexMap<String, Item>;

/// A list of IR items
pub type List = Vec<Item>;

/// A response can either be some `key-value` data representation
/// or an error that occured.
#[derive(Debug)]
pub enum Response {
    /// A data item has a name it will be returned under, and and actual item.
    Data(String, Item),
    Error(String), // FIXME: Use actual error type
}

/// An IR item that either expands to a subtype or leaf-record
#[derive(Debug)]
pub enum Item {
    /// (Parent ID, transformed record as map)
    Map(Option<GraphqlId>, Map),
    List(List),
    Value(PrismaValue),
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
        unimplemented!()
        // self.0.into_iter().fold(vec![], |mut vec, res| {
        //     vec.push(match res {
        //         QueryResult::Read(ReadQueryResult::Single(query)) => {
        //             let query_name = query.name.clone();
        //             match maps::build_map(query) {
        //                 Some(m) => Response::Data(query_name, Item::Map(None, m)),
        //                 None => Response::Data(query_name, Item::Value(PrismaValue::Null)),
        //             }
        //         }
        //         QueryResult::Read(ReadQueryResult::Many(query)) => {
        //             let query_name = query.name.clone();
        //             let query_args = query.query_arguments.clone();
        //             let mut result = lists::build_list(query);

        //             // Trim excess data from the processed result set
        //             trim_records(&mut result, &query_args);
        //             Response::Data(query_name, Item::List(result))
        //         }
        //         QueryResult::Direct(resp) => resp,
        //     });

        //     vec
        // })
    }
}

/// Removes the excess records added to by the database query layer based on the query arguments
/// This would be the right place to add pagination markers (has next page, etc.).
pub fn trim_records(data: &mut Vec<Item>, query_args: &QueryArguments) {
    // The query engine reverses lists when querying for `last`, so we need to reverse again to have the intended order.
    if query_args.last.is_some() {
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
