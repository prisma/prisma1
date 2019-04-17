mod create;
mod delete;
mod relation;
mod update;

pub use create::*;
pub use delete::*;
pub use relation::*;
pub use update::*;

use connector::{mutaction::NestedMutactions, ConnectorResult};
use prisma_models::GraphqlId;
use prisma_query::ast::Query;
use rusqlite::Transaction;

pub trait DatabaseWrite {
    /// Execute a single statement in the database.
    fn execute_one<T>(conn: &Transaction, query: T) -> ConnectorResult<()>
    where
        T: Into<Query>;

    /// Execute a multiple statements in the database.
    fn execute_many<T>(conn: &Transaction, queries: Vec<T>) -> ConnectorResult<()>
    where
        T: Into<Query>;

    fn execute_nested(conn: &Transaction, mutaction: &NestedMutactions, parent_id: &GraphqlId) -> ConnectorResult<()>;
}
