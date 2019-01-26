mod sqlite;

pub use sqlite::Sqlite;

use crate::{
    project::Project,
    protobuf::prisma::GcValue,
    error::Error,
};

pub trait Connector {
    /// A test function to try out the database connection. Triggers `SELECT 1`.
    fn select_1(&self) -> Result<i32, Error>;

    /// Find a certain model where the given fields matches the value.
    fn get_node_by_where(
        &self,
        project: &Project,
        model_name: &str,
        field_name: &str,
        value: &GcValue
    ) -> Result<String, Error>;
}
