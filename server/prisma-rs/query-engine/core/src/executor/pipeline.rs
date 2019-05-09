//! Handles query pipelines with mixed read-write queries

use crate::{ReadQuery, ReadQueryResult, Query, WriteQuery};

/// Represents the lifecycle of a query
pub enum QueryMark {
    Write(WriteQuery),
    Read(ReadQuery),
    PreFetched(ReadQueryResult),
    Replaced(usize),
}

/// A list of QueryMarkers that need to be processed
pub struct QueryPipeline(Vec<QueryMark>);

impl From<Vec<Query>> for QueryPipeline {
    fn from(vec: Vec<Query>) -> Self {
        Self(vec
            .into_iter()
            .map(|q| match q {
                Query::Write(query) => QueryMark::Write(query),
                Query::Read(query) => QueryMark::Read(query),
            })
            .collect())
    }
}