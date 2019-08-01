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

    //    /// Execute a single WriteQuerySet tree, in dependent order
    //    ///
    //    /// During execution, a partial eval will be performed on the results,
    //    /// injecting data into later mutations as needed.
    //    /// Look at `LookAhead` for details!
    //    pub fn exec_one(&self, write_query_set: WriteQuerySet) -> CoreResult<Vec<WriteQueryTreeResult>> {
    //        let mut vec = Vec::new();
    //
    //        match write_query_set {
    //            WriteQuerySet::Query(query) => {
    //                let res = self.write_executor.execute(self.db_name.clone(), query.inner.clone())?;
    //                vec.push(WriteQueryTreeResult {
    //                    inner: res,
    //                    origin: query,
    //                });
    //            }
    //            WriteQuerySet::Dependents { self_, mut next } => {
    //                let res = self.write_executor.execute(self.db_name.clone(), self_.inner.clone())?;
    //                LookAhead::eval_partial(&mut next, &self_, &res)?;
    //
    //                // Then execute next step
    //                vec.append(&mut self.exec_one(*next)?);
    //            }
    //        }
    //
    //        Ok(vec)
    //    }
}
