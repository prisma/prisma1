//! Simple wrapper for WriteQueries
#![warn(warnings)]

use connector::mutaction::{TopLevelDatabaseMutaction, NestedDatabaseMutaction};

/// A top-level write query (mutation)
#[derive(Debug, Clone)]
pub struct WriteQuery {
    /// The actual mutation object being built
    pub inner: TopLevelDatabaseMutaction,

    /// Every WriteQuery is followed by a ReadQuery
    pub query: (),

    /// Nested mutations
    pub nested: Vec<NestedWriteQuery>,
}

/// Nested mutations are slightly different than top-level mutations.
#[derive(Debug, Clone)]
pub struct NestedWriteQuery {
    /// The nested mutation being built
    pub inner: NestedDatabaseMutaction,

    /// Every WriteQuery is followed by a ReadQuery
    pub query: (),

    /// NestedWriteQueries can only have nested children
    pub nested: Vec<NestedWriteQuery>
}