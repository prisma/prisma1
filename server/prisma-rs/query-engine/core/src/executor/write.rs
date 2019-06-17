use crate::{WriteQuery, WriteQueryResult, WriteQuerySet};
use connector::mutaction::{DatabaseMutactionResult, TopLevelDatabaseMutaction};
use connector::{ConnectorResult, DatabaseMutactionExecutor};
use std::sync::Arc;

/// A small wrapper around running WriteQueries
pub struct WriteQueryExecutor {
    pub db_name: String,
    pub write_executor: Arc<DatabaseMutactionExecutor + Send + Sync + 'static>,
}

impl WriteQueryExecutor {
    pub fn execute(&self, mutactions: Vec<WriteQuerySet>) -> ConnectorResult<Vec<WriteQueryResult>> {
        let mut vec = vec![];
        for wq in mutactions {
            match wq {
                WriteQuerySet::Query(query) => {
                    let res = self.write_executor.execute(self.db_name.clone(), query.inner.clone())?;
                    vec.push(WriteQueryResult { inner: res, origin: query });
                },
                WriteQuerySet::Dependents { self_: _, next: _ } => unimplemented!(),
            }
        }

        Ok(vec)
    }
}
