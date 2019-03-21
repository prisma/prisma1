use crate::{
    mutaction::{DatabaseMutaction, DatabaseMutactionResults},
    ConnectorResult,
};
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute_raw(&self, query: String) -> ConnectorResult<Value>;
    fn execute(&self, db_name: String, mutaction: DatabaseMutaction) -> ConnectorResult<DatabaseMutactionResults>;
}
