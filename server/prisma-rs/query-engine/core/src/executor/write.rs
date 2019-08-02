use crate::{CoreError, CoreResult, WriteQueryResultWrapper};
use connector::{UnmanagedDatabaseWriter, WriteQuery};
use std::sync::Arc;

/// A small wrapper around running WriteQueries
pub struct WriteQueryExecutor {
    pub db_name: String,
    pub write_executor: Arc<UnmanagedDatabaseWriter + Send + Sync + 'static>,
}

impl WriteQueryExecutor {
    pub fn execute(&self, write_query: WriteQuery) -> CoreResult<WriteQueryResultWrapper> {
        match write_query {
            WriteQuery::Root(name, alias, wq) => self
                .write_executor
                .execute(self.db_name.clone(), wq)
                .map_err(|err| err.into())
                .map(|result| {
                    WriteQueryResultWrapper {
                        name,
                        alias,
                        result,
                    }
                }),

            _ => Err(CoreError::UnsupportedFeatureError(
                "Attempted to execute nested write query on the root level.".into(),
            )),
        }
    }
}
