use crate::{CoreError, CoreResult};
use connector::{UnmanagedDatabaseWriter, WriteQuery, WriteQueryResult};
use std::sync::Arc;

/// A small wrapper around running WriteQueries
pub struct WriteQueryExecutor {
    pub db_name: String,
    pub write_executor: Arc<UnmanagedDatabaseWriter + Send + Sync + 'static>,
}

impl WriteQueryExecutor {
    pub fn execute(&self, write_query: WriteQuery) -> CoreResult<WriteQueryResult> {
        match write_query {
            WriteQuery::Root(wq) => self
                .write_executor
                .execute(self.db_name.clone(), wq)
                .map_err(|err| err.into()),

            _ => Err(CoreError::UnsupportedFeatureError(
                "Attempted to execute nested write query on the root level.".into(),
            )),
        }
    }
}
