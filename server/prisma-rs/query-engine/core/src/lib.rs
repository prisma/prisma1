#![warn(warnings)] // todo deny warnings once done

// #[macro_use]
// extern crate log;

#[macro_use]
extern crate debug_stub_derive;

#[macro_use]
extern crate lazy_static;

mod error;

pub mod executor;
pub mod query_builders;
pub mod query_document;
pub mod result_ir;
pub mod schema;

pub use error::*;
pub use executor::QueryExecutor;
pub use schema::*;

use connector::{Query, ReadQueryResult};

pub type CoreResult<T> = Result<T, CoreError>;

/// Temporary type to work around current dependent execution limitations.
pub type QueryPair = (Query, ResultResolutionStrategy);

pub enum ResultResolutionStrategy {
    /// Resolve the actual result by evaluating another query
    Query(Query),
    CoerceInto(OutputTypeRef),
    None,
}

/// Temporary result type to tie together dependent query execution and the old result structures
#[derive(Debug)]
pub enum QueryResult {
    Read(ReadQueryResult),
    Direct(result_ir::Response),
}
