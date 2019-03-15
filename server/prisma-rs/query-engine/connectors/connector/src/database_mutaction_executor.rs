use serde_json::Value;
use crate::mutaction::{DatabaseMutactionResults, DatabaseMutaction};
use prisma_common::PrismaResult;

pub trait DatabaseMutactionExecutor {
    fn execute_raw(&self, query: String) -> Value;
    fn execute(&self, mutaction: DatabaseMutaction) -> PrismaResult<DatabaseMutactionResults>;
}
