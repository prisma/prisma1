use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{self, NO_PARAMS};

use crate::{
    project::Project,
    protobuf::prisma::GcValue,
    error::Error,
    connector::Connector,
};

pub struct Sqlite {
    pool: r2d2::Pool<SqliteConnectionManager>,
}

impl Sqlite {
    pub fn new(database_file: &str, connection_limit: u32) -> Result<Sqlite, Error> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::file(database_file))?;

        Ok(Sqlite { pool })
    }
}

impl Connector for Sqlite {
    fn select_1(&self) -> Result<i32, Error> {
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
    ) -> Result<String, Error> {
        unimplemented!()
    }
}
