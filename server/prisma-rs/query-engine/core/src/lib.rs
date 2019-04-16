#![deny(warnings)]

mod builders;
mod error;
mod formatters;
mod query_ast;
mod query_executor;

pub use builders::*;
pub use error::*;
pub use formatters::*;
pub use query_ast::*;
pub use query_executor::*;

pub type CoreResult<T> = Result<T, CoreError>;
