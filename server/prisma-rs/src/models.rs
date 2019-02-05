mod project;
mod schema;
mod model;
mod field;

pub use project::*;
pub use schema::*;
pub use model::*;
pub use field::*;

pub trait Renameable {
    fn db_name(&self) -> &str;
}
