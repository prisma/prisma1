use crate::{builders::LookAhead, CoreResult, WriteQueryResult, WriteQuerySet};
use connector::DatabaseMutactionExecutor;

use std::sync::Arc;

/// A small wrapper around running WriteQueries
pub struct WriteQueryExecutor {
    pub db_name: String,
    pub write_executor: Arc<DatabaseMutactionExecutor + Send + Sync + 'static>,
}

impl WriteQueryExecutor {

    /// A convenience function around `exec_one`
    pub fn execute(&self, mutactions: Vec<WriteQuerySet>) -> CoreResult<Vec<WriteQueryResult>> {
        let mut vec = vec![];
        for wq in mutactions {
            vec.append(&mut self.exec_one(wq)?);
        }

        Ok(vec)
    }

    /// Execute a single WriteQuerySet tree, in dependent order
    ///
    /// During execution, a partial eval will be performed on the results,
    /// injecting data into later mutations as needed.
    /// Look at `LookAhead` for details!
    pub fn exec_one(&self, mutaction: WriteQuerySet) -> CoreResult<Vec<WriteQueryResult>> {
        let mut vec = Vec::new();

        match mutaction {
            WriteQuerySet::Query(query) => {
                let res = self.write_executor.execute(self.db_name.clone(), query.inner.clone())?;
                vec.push(WriteQueryResult {
                    inner: res,
                    origin: query,
                });
            }
            WriteQuerySet::Dependents { self_, mut next } => {
                let res = self.write_executor.execute(self.db_name.clone(), self_.inner.clone())?;
                LookAhead::eval_partial(&mut next, &self_, &res)?;

                // Then execute next step
                vec.append(&mut self.exec_one(*next)?);
            }
        }

        Ok(vec)
    }
}
