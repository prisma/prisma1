mod sqlite;

pub use sqlite::Sqlite;

pub type PrismaConnector = Box<dyn Connector + Send + Sync + 'static>;

use crate::{models::ScalarField, PrismaResult, PrismaValue};

pub trait Connector {
    /// Find a certain model where the given fields matches the value.
    fn get_node_by_where(
        &self,
        database_name: &str,
        table_name: &str,
        selected_fields: &[&ScalarField],
        query_condition: (&ScalarField, &PrismaValue),
    ) -> PrismaResult<Vec<PrismaValue>>;
}
