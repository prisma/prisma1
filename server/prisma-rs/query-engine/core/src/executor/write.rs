use crate::{WriteQuery, WriteQueryResult};
use connector::mutaction::{DatabaseMutactionResult, TopLevelDatabaseMutaction};
use connector::{ConnectorResult, DatabaseMutactionExecutor};
use std::sync::Arc;

/// A small wrapper around running WriteQueries
pub struct WriteQueryExecutor {
    pub db_name: String,
    pub write_executor: Arc<DatabaseMutactionExecutor + Send + Sync + 'static>,
}

impl WriteQueryExecutor {
    pub fn execute(&self, mutactions: Vec<WriteQuery>) -> ConnectorResult<Vec<WriteQueryResult>> {
        let mut vec = vec![];
        for wq in mutactions {
            let res = self.write_executor.execute(self.db_name.clone(), wq.inner.clone())?;
            vec.push(WriteQueryResult { inner: res, origin: wq });
        }

        Ok(vec)
    }
}
