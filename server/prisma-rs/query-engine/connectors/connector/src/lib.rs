#![deny(warnings)]
#![macro_use]
extern crate failure_derive;

pub mod error;
pub mod filter;
pub mod mutaction;

mod compare;
mod data_resolver;
mod database_mutaction_executor;
mod query_arguments;

pub use compare::*;
pub use data_resolver::*;
pub use database_mutaction_executor::*;
pub use query_arguments::*;

pub type ConnectorResult<T> = Result<T, error::ConnectorError>;
