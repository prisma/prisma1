use crate::{
    mutaction::{DatabaseMutactionResult, TopLevelDatabaseMutaction},
    ConnectorResult,
};
use serde_json::Value;

/// Methods for writing data.
pub trait DatabaseMutactionExecutor {
    /// Execute raw SQL string without any safety guarantees, returning the result as JSON.
    fn execute_raw(&self, db_name: String, query: String) -> ConnectorResult<Value>;

    /// Executes the mutaction and all nested mutactions, returning the result
    /// of the topmost mutaction.
    fn execute(
        &self,
        db_name: String,
        mutaction: TopLevelDatabaseMutaction,
    ) -> ConnectorResult<DatabaseMutactionResult>;
}
