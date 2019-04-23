use crate::{
    mutaction::{DatabaseMutaction, DatabaseMutactionResults},
    ConnectorResult,
};
use prisma_models::*;
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute_raw(&self, _query: String) -> ConnectorResult<Value>;

    fn execute(
        &self,
        db_name: String,
        mutaction: DatabaseMutaction,
        parent_id: Option<GraphqlId>, // TODO: we don't need this when we handle the whole mutaction in here.
    ) -> ConnectorResult<DatabaseMutactionResults>;
}
