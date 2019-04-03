use crate::{DatabaseExecutor, DatabaseRead, Sqlite};
use connector::ConnectorResult;
use prisma_query::ast::Select;
use rusqlite::Row;

impl DatabaseExecutor for Sqlite {
    fn with_rows<F, T>(&self, query: Select, db_name: &str, f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
    {
        self.with_transaction(db_name, |conn| Self::query(conn, query, f))
    }
}
