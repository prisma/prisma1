use crate::mutaction::{DatabaseMutaction, DatabaseMutactionResults};
use prisma_common::PrismaResult;
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute_raw(&self, query: String) -> PrismaResult<Value>;
    fn execute(&self, db_name: String, mutaction: DatabaseMutaction) -> PrismaResult<DatabaseMutactionResults>;
}
