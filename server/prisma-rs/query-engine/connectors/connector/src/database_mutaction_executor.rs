use crate::{
    mutaction::{DatabaseMutactionResult, TopLevelDatabaseMutaction},
    ConnectorResult,
};
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute_raw(&self, _query: String) -> ConnectorResult<Value>;

    fn execute(
        &self,
        db_name: String,
        mutaction: TopLevelDatabaseMutaction,
    ) -> ConnectorResult<DatabaseMutactionResult>;
}
