//! Handles query pipelines with mixed read-write queries
//!
//! The general information flow is as follows:
//!
//! - Run pre-fetch queries for every delete
//! - Run mutations
//! - Run other queries
//!
//! When running pre-fetch queries, the results need to be cached
//! and also those queries need to first be derived from the WriteQuery
//! they are based on.
//!
//! The `pipeline` module itself doesn't do this and relies on the
//! mutation builders for a lot of this. But the general lifecycle
//! of queries is implemented here

#![allow(warnings)]

use crate::{Query, ReadQuery, ReadQueryResult, WriteQuery};
use indexmap::IndexMap;

/// Represents the lifecycle of a query
///
/// The way that queries are handled is encoded in the
/// enum variants. Check the module documentation on
/// more detail for what order queries get executed in.
///
/// This type is to be considered an implementation detail
/// of the `QueryPipeline` type defined below.
///
// TODO: maybe rename?
#[derive(Debug)]
enum Stage {
    /// Stores a simple read query
    Read(ReadQuery),
    /// Store a write query and an index
    Write(usize, WriteQuery),
    /// Acts as a placeholder to hand out `WriteQuery` ownership.
    /// The index is used to re-associate slots further down a pipeline
    WriteMark(usize),
    /// Stores the intermediate result of pre-feteching records
    /// before executing destructive writes (i.e. deletes)
    PreFetched(WriteQuery, ReadQueryResult),
    /// Encodes the end-result of a local pipeline
    Done(ReadQueryResult),
}

/// A list of Queryers that need to be processed
///
/// Generally the order to call the associated functions in is
///
/// 1. `prefetch()`
/// 2. `store_prefetch()`
/// 3. `get_writes()`
/// 4. `get_reads()`
/// 5. `consume()`
pub struct QueryPipeline(Vec<Stage>);

impl From<Vec<Query>> for QueryPipeline {
    fn from(vec: Vec<Query>) -> Self {
        Self(
            vec.into_iter()
                .zip(0..)
                .map(|(q, idx)| match q {
                    Query::Write(query) => Stage::Write(idx, query),
                    Query::Read(query) => Stage::Read(query),
                })
                .collect(),
        )
    }
}

impl QueryPipeline {
    /// Returns all queries that need pre-fetching data
    ///
    /// Under the hood this generates a new `ReadQuery` for every
    /// `WriteQuery` which destructively acts on the database (i.e. deletes).
    ///
    /// **Important:** you need to call `store_prefetch` with the results
    pub fn prefetch(&self) -> IndexMap<usize, ReadQuery> {
        self.0.iter().fold(IndexMap::new(), |mut map, query| {
            if let Stage::Write(idx, query) = query {
                if let Some(fetch) = query.generate_prefetch() {
                    map.insert(*idx, fetch);
                }
            }
            map
        })
    }

    /// Takes the set of pre-fetched results and re-associates it into the pipeline
    pub fn store_prefetch(&mut self, mut data: IndexMap<usize, ReadQueryResult>) {
        self.0 = std::mem::replace(&mut self.0, vec![]) // A small hack around ownership
            .into_iter()
            .map(|stage| match stage {
                Stage::Write(idx, query) => match data.remove(&idx) {
                    Some(result) => Stage::PreFetched(query, result),
                    None => Stage::Write(idx, query),
                },
                stage => stage,
            })
            .collect();

        // This _should_ never happen but we should warn-log it anyway
        if data.len() != 0 {
            warn!("Unused pre-fetch results in query pipeline!");
        }
    }

    /// Get all write queries to execute
    ///
    /// Some of them will have an index associated to them. This is because
    /// they will return a ReadQuery which has not been executed yet.
    ///
    /// This marker should also be used to determine which WriteQuery
    /// must result in another ReadQuery and the pipeline then uses this
    /// information to re-associate data to be in the expected order.
    pub fn get_writes(&mut self) -> Vec<(WriteQuery, Option<usize>)> {
        let (rest, writes) = std::mem::replace(&mut self.0, vec![]).into_iter().fold(
            (vec![], vec![]),
            |(mut rest, mut writes), stage| {
                match stage {
                    Stage::Write(idx, query) => {
                        rest.push(Stage::WriteMark(idx));
                        writes.push((query, Some(idx)));
                    }
                    Stage::PreFetched(query, data) => {
                        rest.push(Stage::Done(data));
                        writes.push((query, None));
                    }
                    Stage::Read(query) => rest.push(Stage::Read(query)),
                    stage => panic!("Unexpected pipeline stage {:?} in function `get_writes`", stage),
                };
                (rest, writes)
            },
        );

        self.0 = rest;
        writes
    }
}
