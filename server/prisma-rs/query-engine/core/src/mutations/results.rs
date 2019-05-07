//! WriteQuery results are kinda special
#![warn(warnings)]

use connector::mutaction::DatabaseMutactionResult;

/// A structure that encodes the results from a database mutation
pub struct WriteQueryResult {

    /// The immediate mutation return
    pub inner: DatabaseMutactionResult,

    /// Nested mutation results
    pub nested: Vec<WriteQueryResult>,

    /// Associated selection-set for this level
    pub query: (),
}