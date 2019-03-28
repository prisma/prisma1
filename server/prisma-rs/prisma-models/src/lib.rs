#![deny(warnings)]

#[macro_use]
extern crate serde_derive;
#[macro_use]
extern crate debug_stub_derive;
#[macro_use]
extern crate diesel;

mod error;
mod field;
mod fields;
mod model;
mod node;
mod order_by;
mod prisma_args;
mod prisma_value;
mod project;
mod relation;
mod scalar_list_table;
mod schema;
mod selected_fields;

pub mod prelude;

pub use error::*;
pub use field::*;
pub use fields::*;
pub use model::*;
pub use node::*;
pub use order_by::*;
pub use prisma_args::*;
pub use prisma_args::*;
pub use prisma_value::*;
pub use project::*;
pub use relation::*;
pub use scalar_list_table::*;
pub use schema::*;
pub use selected_fields::*;

pub type DomainResult<T> = Result<T, DomainError>;
