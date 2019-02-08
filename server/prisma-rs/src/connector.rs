mod sqlite;

use crate::{models::Model, protobuf::prisma::Node};

pub use sqlite::Sqlite;
pub type PrismaConnector = Box<dyn Connector + Send + Sync + 'static>;

use crate::{
    models::{Field, ScalarField},
    protobuf::prisma::QueryArguments,
    PrismaResult, PrismaValue,
};

/// Trait responsible for fetching rows from the database.
pub trait Connector {
    /// Find a certain model where the given fields matches the value.
    fn get_node_by_where(
        &self,
        database_name: &str,
        model: &Model,
        selected_fields: &[&ScalarField],
        query_conditions: (&ScalarField, &PrismaValue),
    ) -> PrismaResult<Node>;

    /// Find all nodes with the given query arguments.
    fn get_nodes(
        &self,
        database_name: &str,
        table_name: &str,
        selected_fields: &[Field],
        query_arguments: &QueryArguments,
    ) -> PrismaResult<Vec<Node>>;
}
