#![deny(warnings)]

mod builders;
mod error;
mod query_ast;
mod query_executor;
pub mod ir;

pub use builders::*;
pub use error::*;
pub use query_ast::*;
pub use query_executor::*;

pub type CoreResult<T> = Result<T, CoreError>;
