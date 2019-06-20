#![deny(warnings)]

#[macro_use]
extern crate serde_derive;

#[macro_use]
extern crate debug_stub_derive;

mod datamodel_converter;
mod error;
mod field;
mod fields;
mod internal_data_model;
mod model;
mod order_by;
mod prisma_args;
mod prisma_value;
mod project;
mod record;
mod relation;
mod scalar_list_table;
mod selected_fields;

pub mod prelude;

pub use datamodel_converter::*;
pub use error::*;
pub use field::*;
pub use fields::*;
pub use internal_data_model::*;
pub use model::*;
pub use order_by::*;
pub use prisma_args::*;
pub use prisma_args::*;
pub use prisma_value::*;
pub use project::*;
pub use record::*;
pub use relation::*;
pub use scalar_list_table::*;
pub use selected_fields::*;

pub type DomainResult<T> = Result<T, DomainError>;
