use crate::{mutaction::*, ConnectorResult};
use prisma_models::GraphqlId;
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute_raw(&self, query: String) -> ConnectorResult<Value>;
    fn execute(&self, db_name: String, mutaction: DatabaseMutaction) -> ConnectorResult<DatabaseMutactionResults>;

    fn execute_create(&self, db_name: String, mutaction: &CreateNode) -> ConnectorResult<GraphqlId>;
}
