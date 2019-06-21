#![deny(warnings)]
#![macro_use]
extern crate failure_derive;

pub mod error;
pub mod filter;
pub mod write_query;

mod compare;
mod database_reader;
mod database_writer;
mod query_arguments;

pub use compare::*;
pub use database_reader::*;
pub use database_writer::*;
pub use query_arguments::*;

pub type ConnectorResult<T> = Result<T, error::ConnectorError>;
