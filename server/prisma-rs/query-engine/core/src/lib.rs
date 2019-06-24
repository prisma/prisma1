#![warn(warnings)]

#[macro_use]
extern crate log;

#[macro_use]
extern crate debug_stub_derive;

#[macro_use]
extern crate lazy_static;

mod error;
mod executor;
mod query_builders;
mod read_query_ast;
mod read_query_result;

pub mod query_ir;
pub mod result_ir;
pub mod schema;

pub use error::*;
pub use executor::*;
pub use query_builders::*;
pub use read_query_ast::*;
pub use read_query_result::*;
pub use schema::*;

pub type CoreResult<T> = Result<T, CoreError>;

/// A type wrapper around read and write queries
#[derive(Debug, Clone)]
pub enum Query {
    Read(ReadQuery),
    Write(WriteQuerySet),
}
