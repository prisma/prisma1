mod sqlite;

pub use sqlite::Sqlite;

use crate::{querying::NodeSelector, PrismaResult, PrismaValue};

pub trait Connector {
    /// Find a certain model where the given fields matches the value.
    fn get_node_by_where(
        &self,
        database_name: &str,
        selector: &NodeSelector,
    ) -> PrismaResult<Vec<PrismaValue>>;
}
