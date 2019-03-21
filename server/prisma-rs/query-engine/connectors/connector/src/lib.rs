#![deny(warnings)]
#![macro_use]
extern crate failure_derive;

mod data_resolver;
mod database_mutaction_executor;
mod error;
mod filter;
mod mutaction;
mod node_selector;
mod query_arguments;

pub use data_resolver::*;
pub use database_mutaction_executor::*;
pub use error::*;
pub use filter::*;
pub use mutaction::*;
pub use mutaction::*;
pub use node_selector::*;
pub use query_arguments::*;

pub type ConnectorResult<T> = Result<T, ConnectorError>;
