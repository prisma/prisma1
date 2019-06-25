#![deny(warnings)]
#![macro_use]
extern crate failure_derive;

pub mod error;
pub mod filter;

mod compare;
mod interfaces;
mod query_arguments;
mod query_ast;
mod result_ast;

pub use compare::*;
pub use interfaces::*;
pub use query_arguments::*;
pub use query_ast::*;
pub use result_ast::*;

pub type ConnectorResult<T> = Result<T, error::ConnectorError>;
