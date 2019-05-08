//! Simple wrapper for WriteQueries

use connector::mutaction::{TopLevelDatabaseMutaction, NestedDatabaseMutaction};
use graphql_parser::query::Field;

/// A top-level write query (mutation)
#[derive(Debug, Clone)]
pub struct WriteQuery {
    /// The actual mutation object being built
    pub inner: TopLevelDatabaseMutaction,

    /// Required to create following ReadQuery
    pub field: Field,

    /// Nested mutations
    pub nested: Vec<NestedWriteQuery>,
}

/// Nested mutations are slightly different than top-level mutations.
#[derive(Debug, Clone)]
pub struct NestedWriteQuery {
    /// The nested mutation being built
    pub inner: NestedDatabaseMutaction,

    /// Required to create following ReadQuery
    pub field: Field,

    /// NestedWriteQueries can only have nested children
    pub nested: Vec<NestedWriteQuery>
}