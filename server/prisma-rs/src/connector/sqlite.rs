use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{self, NO_PARAMS};

use crate::{
    project::Project,
    protobuf::prisma::GcValue,
    error::Error,
    connector::Connector,
    PrismaResult,
};

pub struct Sqlite {
    pool: r2d2::Pool<SqliteConnectionManager>,
}

impl Sqlite {
    /// Creates a new SQLite pool. The database file is created automatically if
    /// it doesn't exist yet.
    pub fn new(database_file: &str, connection_limit: u32) -> PrismaResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::file(database_file))?;

        Ok(Sqlite { pool })
    }
}

impl Connector for Sqlite {
    fn select_1(&self) -> PrismaResult<i32> {
        let conn = self.pool.get()?;
        let mut stmt = conn.prepare("SELECT 1")?;
        let mut rows = stmt.query_map(NO_PARAMS, |row| row.get(0))?;

        match rows.next() {
            Some(r) => Ok(r?),
            None => Err(Error::NoResultsError)
        }
    }

    fn get_node_by_where(
        &self,
        _project: &Project,
        _model_name: &str,
        _field_name: &str,
        _value: &GcValue,
    ) -> PrismaResult<String> {
        unimplemented!()
    }
}
