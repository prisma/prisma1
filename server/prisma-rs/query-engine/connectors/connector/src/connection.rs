use crate::{
    error::ConnectorError,
    row::{PrismaRow, ToPrismaRow},
    ConnectorResult,
};
use prisma_models::TypeIdentifier;

#[cfg(feature = "sqlite")]
use rusqlite::Connection as SqliteConnection;

#[cfg(feature = "sqlite")]
use prisma_query::{
    ast::{Query, Select},
    visitor::{self, Visitor},
};

/// handled per-database basis, `Transaction` providing a minimal interface over
/// different databases.
pub trait Transaction {
    /// Write to the database, expecting no result data. On success, returns the
    /// number of rows that were changed, inserted, or deleted.
    fn write(&mut self, q: Query) -> ConnectorResult<usize>;

    /// Select multiple rows from the database.
    fn read(&mut self, q: Select, idents: &[TypeIdentifier]) -> ConnectorResult<Vec<PrismaRow>>;

    /// Select one row from the database.
    fn read_one(&mut self, q: Select, idents: &[TypeIdentifier]) -> ConnectorResult<PrismaRow> {
        self.read(q.limit(1), idents)?
            .into_iter()
            .next()
            .ok_or(ConnectorError::NodeDoesNotExist)
    }

    /// Read the first column as an integer.
    fn read_int(&mut self, q: Select) -> ConnectorResult<i64>;
}

#[cfg(feature = "sqlite")]
impl<'a> Transaction for SqliteTransaction<'a> {
    fn write(&mut self, q: Query) -> ConnectorResult<usize> {
        let (sql, params) = visitor::Sqlite::build(q);

        let mut stmt = self.prepare_cached(&sql)?;
        let changes = stmt.execute(params)?;

        Ok(changes)
    }

    fn read(&mut self, q: Select, idents: &[TypeIdentifier]) -> ConnectorResult<Vec<PrismaRow>> {
        let (sql, params) = visitor::Sqlite::build(q);

        let mut stmt = self.prepare_cached(&sql)?;
        let mut rows = stmt.query(params)?;
        let mut result = Vec::new();

        while let Some(row) = rows.next() {
            result.push(row?.to_prisma_row(idents)?);
        }

        Ok(result)
    }

    fn read_int(&mut self, q: Select) -> ConnectorResult<i64> {
        let (sql, params) = visitor::Sqlite::build(q);

        let mut stmt = self.prepare_cached(&sql)?;
        let mut rows = stmt.query(params)?;

        if let Some(row) = rows.next() {
            Ok(row?.get_checked(1)?)
        } else {
            Ok(0)
        }
    }
}
