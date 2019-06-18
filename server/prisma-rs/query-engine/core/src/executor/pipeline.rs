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

use crate::{Query, ReadQuery, ReadQueryResult, WriteQuery, WriteQueryResult, MutationSet};
use indexmap::IndexMap;
use std::mem::replace;

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
    Read(usize, ReadQuery),
    /// Acts as a placeholder for when read queries are executed
    ReadMark(usize),
    /// Store a write query and an index
    Write(usize, MutationSet),
    /// Stores the intermediate result of pre-feteching records
    /// before executing destructive writes (i.e. deletes)
    PreFetched(MutationSet, ReadQueryResult),
    /// Encodes the end-result of a local pipeline
    Done(ReadQueryResult),
}

/// A list of Queries and their stage that need to be processed
///
/// Generally the order to call the associated functions in is
///
/// 1. `prefetch()`
/// 2. `store_prefetch()`
/// 3. `get_writes()`
/// 4. `store_write_returns()`
/// 5. `get_reads()`
/// 6. `store_reads()`
/// 7. `consume()`
#[derive(Debug)]
pub struct QueryPipeline(Vec<Stage>);

impl From<Vec<Query>> for QueryPipeline {
    fn from(vec: Vec<Query>) -> Self {
        Self(
            vec.into_iter()
                .zip(0..)
                .map(|(q, idx)| match q {
                    Query::Write(query) => Stage::Write(idx, query),
                    Query::Read(query) => Stage::Read(idx, query),
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
    /// It's recommended to iterate over the map, without disturbing key entries
    /// because these are used later on to re-associate data into the pipeline.
    ///
    /// **Remember:** you need to call `store_prefetch` with the results
    pub fn prefetch(&self) -> IndexMap<usize, ReadQuery> {
        self.0.iter().fold(IndexMap::new(), |mut map, query| {
            if let Stage::Write(idx, MutationSet::Query(query)) = query {
                if let Some(fetch) = query.generate_prefetch() {
                    map.insert(*idx, fetch);
                }
            }
            map
        })
    }

    /// Takes the set of pre-fetched results and re-associates it into the pipeline
    pub fn store_prefetch(&mut self, mut data: IndexMap<usize, ReadQueryResult>) {
        self.0 = replace(&mut self.0, vec![]) // A small hack around ownership
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
    /// they will return a ReadQuery which has not yet been executed.
    ///
    /// This marker should also be used to determine which WriteQuery
    /// must result in another ReadQuery and the pipeline then uses this
    /// information to re-associate data to be in the expected order.
    pub fn get_writes(&mut self) -> Vec<(Option<usize>, MutationSet)> {
        let (rest, writes) = replace(&mut self.0, vec![]) // A small hack around ownership
            .into_iter()
            .fold((vec![], vec![]), |(mut rest, mut writes), stage| {
                match stage {
                    Stage::Write(idx, query) => {
                        rest.push(Stage::ReadMark(idx));
                        writes.push((Some(idx), query));
                    }
                    Stage::PreFetched(query, data) => {
                        rest.push(Stage::Done(data));
                        writes.push((None, query));
                    }
                    Stage::Read(idx, query) => rest.push(Stage::Read(idx, query)),
                    stage => panic!("Unexpected pipeline stage {:?} in function `get_writes`", stage),
                };
                (rest, writes)
            });

        self.0 = rest;
        writes
    }

    /// Store relevant WriteQuery return values and generate ReadQueries
    pub fn process_writes(&mut self, writes: Vec<(Option<usize>, WriteQueryResult)>) -> Vec<(usize, ReadQuery)> {
        writes
            .into_iter()
            .filter_map(|(idx, result)| {
                let read_result = result.generate_result();
                let origin = result.origin;
                let inner = result.inner;

                match (idx, origin.generate_read(inner)) {
                    (Some(idx), Some(read)) => Some((idx, read)),
                    (Some(idx), None) => {
                        self.0.remove(idx);
                        self.0.insert(idx, Stage::Done(read_result.unwrap()));

                        // Return None to exclude from the list
                        None
                    }
                    (None, None) => None, // We just filter out everything else
                    (_, _) => unreachable!(),
                }
            })
            .collect()
    }

    /// Store read results at placeholder locations in the pipeline
    ///
    /// This function is invoked both after what the execution engines
    /// does with the result of `get_writes()` and normal reads provided
    /// by `get_reads()`.
    pub fn store_reads(&mut self, mut data: IndexMap<usize, ReadQueryResult>) {
        self.0 = replace(&mut self.0, vec![]) // A small hack around ownership
            .into_iter()
            .map(|stage| match stage {
                Stage::ReadMark(idx) => match data.remove(&idx) {
                    Some(result) => Stage::Done(result),
                    None => panic!("Expected data entry for index `{}`, but `None` was found!", idx),
                },
                stage => stage,
            })
            .collect();

        // This _should_ never happen but we should warn-log it anyway
        if data.len() != 0 {
            warn!("Unused pre-fetch results in query pipeline!");
        }
    }

    /// Get all remaining read queries and their pipeline indices
    ///
    /// Be sure to call `store_reads()` with query results!
    pub fn get_reads(&mut self) -> Vec<(usize, ReadQuery)> {
        let (rest, reads) = replace(&mut self.0, vec![]) // A small hack around ownership
            .into_iter()
            .fold((vec![], vec![]), |(mut rest, mut reads), stage| {
                match stage {
                    Stage::Read(idx, query) => {
                        rest.push(Stage::ReadMark(idx));
                        reads.push((idx, query));
                    }
                    Stage::Done(data) => rest.push(Stage::Done(data)),
                    stage => panic!("Unexpected pipeline stage {:?} in function `get_reads`", stage),
                };
                (rest, reads)
            });

        self.0 = rest;
        reads
    }

    /// Consumes the pipeline into a list of results
    pub fn consume(self) -> Vec<ReadQueryResult> {
        self.0
            .into_iter()
            .map(|stage| match stage {
                Stage::Done(data) => data,
                stage => panic!(
                    "Called `consume` on non-final pipeline containing {:?} stage items!",
                    stage
                ),
            })
            .collect()
    }
}
