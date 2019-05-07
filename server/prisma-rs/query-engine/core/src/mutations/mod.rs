//! Mutation builder module
#![warn(warnings)]

mod results;
mod builder;
mod ast;

pub use results::*;
pub use builder::*
pub use ast::*;

use connector::{DatabaseMutactionExecutor, ConnectorResult};
use std::sync::Arc;

/// A small wrapper around running WriteQueries
pub struct WriteQueryExecutor {
    pub db_name: String,
    pub write_executor: Arc<DatabaseMutactionExecutor + Send + Sync + 'static>,
}

impl WriteQueryExecutor {
    pub fn execute(&self, mutaction: TopLevelDatabaseMutaction) -> ConnectorResult<DatabaseMutactionResult> {
        self.write_executor.execute(self.db_name.clone(), mutaction)
    }
}
