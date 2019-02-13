mod field;
mod fields;
mod model;
mod project;
mod schema;

pub mod prelude;

pub use field::*;
pub use fields::*;
pub use model::*;
pub use project::*;
pub use schema::*;

pub trait Renameable {
    fn db_name(&self) -> &str;
}
