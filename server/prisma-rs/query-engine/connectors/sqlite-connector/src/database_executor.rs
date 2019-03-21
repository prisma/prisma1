use connector::ConnectorResult;
use prisma_query::ast::Select;
use rusqlite::Row;

pub trait DatabaseExecutor {
    fn with_rows<F, T>(&self, query: Select, db_name: String, f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>;
}
