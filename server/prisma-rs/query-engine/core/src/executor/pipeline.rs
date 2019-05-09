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

use crate::{Query, ReadQuery, ReadQueryResult, WriteQuery};
use std::collections::HashMap;

/// Represents the lifecycle of a query
pub enum QueryMark {
    /// Store a write query and an index to re-associate data later on
    Write(usize, WriteQuery),
    /// Stores a simple read query
    Read(ReadQuery),
    /// Stores the intermediate result of pre-feteching records
    /// before executing destructive writes (i.e. deletes)
    PreFetched(WriteQuery, ReadQueryResult),
    /// Encodes the end-result of a local pipeline
    Done(ReadQueryResult),
}

/// A list of QueryMarkers that need to be processed
pub struct QueryPipeline(Vec<QueryMark>);

impl From<Vec<Query>> for QueryPipeline {
    fn from(vec: Vec<Query>) -> Self {
        Self(
            vec.into_iter()
                .zip(0..)
                .map(|(q, idx)| match q {
                    Query::Write(query) => QueryMark::Write(idx, query),
                    Query::Read(query) => QueryMark::Read(query),
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
    pub fn prefetch(&self) -> Vec<(usize, ReadQuery)> {
        self.0.iter().fold(vec![], |mut vec, query| {
            if let QueryMark::Write(idx, query) = query {
                if let Some(fetch) = query.generate_prefetch() {
                    vec.push((*idx, fetch));
                }
            }
            vec
        })
    }

    /// Takes the set of pre-fetched results and re-associates it into the pipeline
    pub fn store_prefetch(&mut self, mut data: HashMap<usize, ReadQueryResult>) {
        self.0 = std::mem::replace(&mut self.0, vec![]) // A small hack around ownership
            .into_iter()
            .map(|mark| match mark {
                QueryMark::Write(idx, query) => match data.remove(&idx) {
                    Some(result) => QueryMark::PreFetched(query, result),
                    None => QueryMark::Write(idx, query),
                },
                mark => mark,
            })
            .collect();

        // This _should_ never happen but we should warn-log it anyway
        if data.len() != 0 {
            warn!("Unused pre-fetch results in query pipeline!");
        }
    }
}
