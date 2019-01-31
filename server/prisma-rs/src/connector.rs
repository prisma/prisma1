mod sqlite;

pub use sqlite::Sqlite;

use crate::{
    querying::{NodeSelector, PrismaValue},
    PrismaResult,
};

pub trait Connector {
    /// A test function to try out the database connection. Triggers `SELECT 1`.
    fn select_1(&self) -> PrismaResult<i32>;

    /// Find a certain model where the given fields matches the value.
    fn get_node_by_where(
        &self,
        database_name: &str,
        selector: &NodeSelector,
    ) -> PrismaResult<Vec<PrismaValue>>;
}
