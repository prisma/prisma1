mod field;
mod model;
mod project;
mod schema;

pub use field::*;
pub use model::*;
pub use project::*;
pub use schema::*;

pub trait Renameable {
    fn db_name(&self) -> &str;
}
