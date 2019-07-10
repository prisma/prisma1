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

pub type CoreResult<T> = Result<T, CoreError>;
