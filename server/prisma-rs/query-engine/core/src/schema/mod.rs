#![warn(warnings)]

mod builder;
mod query_schema;
mod utils;

pub use builder::*;
pub use query_schema::*;
pub use utils::*;

/// Trait that should be implemented in order to be able to render a query schema.
pub trait QuerySchemaRenderer {
  fn render(query_schema: &QuerySchema) -> String;
}
