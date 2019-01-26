mod sqlite;

pub use sqlite::Sqlite;

use crate::{
    project::Project,
    protobuf::prisma::GcValue,
    error::Error,
};

pub trait Connector {
    fn select_1(&self) -> Result<i32, Error>;

    fn get_node_by_where(
        &self,
        project: &Project,
        model_name: &str,
        field_name: &str,
        value: &GcValue
    ) -> Result<String, Error>;
}
