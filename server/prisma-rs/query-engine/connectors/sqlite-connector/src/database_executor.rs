use prisma_common::PrismaResult;
use prisma_query::ast::Select;
use rusqlite::Row;

pub trait DatabaseExecutor {
    fn with_rows<F, T>(&self, query: Select, db_name: String, f: F) -> PrismaResult<Vec<T>>
    where
        F: FnMut(&Row) -> PrismaResult<T>;
}
