#![deny(warnings)]
#![macro_use]
extern crate failure_derive;

pub mod error;
pub mod filter;
pub mod query_ast;
pub mod result_ast;

mod compare;
mod interfaces;
mod query_arguments;

pub use compare::*;
pub use interfaces::*;
pub use query_arguments::*;
pub use query_ast::*;
pub use result_ast::*;

pub type Result<T> = std::result::Result<T, error::ConnectorError>;
