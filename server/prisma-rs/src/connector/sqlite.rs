use r2d2;
use r2d2_sqlite::SqliteConnectionManager;

use rusqlite::{types::ToSql, NO_PARAMS};

use crate::{
    connector::Connector, error::Error, project::Project, querying::NodeSelector, PrismaResult,
};

type Connection = r2d2::PooledConnection<SqliteConnectionManager>;

pub struct Sqlite {
    pool: r2d2::Pool<SqliteConnectionManager>,
}

impl Sqlite {
    pub fn new(connection_limit: u32) -> PrismaResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        Ok(Sqlite { pool })
    }

    fn find_or_create_database(conn: &mut Connection, db_name: &str) -> PrismaResult<()> {
        let mut stmt = conn.prepare("PRAGMA database_list")?;

        let mut databases = stmt.query_map(NO_PARAMS, |row| {
            let name: String = row.get(1);
            name
        })?;

        let existing = dbg!(databases.find(|res| match res {
            Ok(ref name) => name == db_name,
            _ => false,
        }));

        if existing.is_none() {
            let path = format!("db/{}", db_name);
            dbg!(conn.execute("ATTACH DATABASE ? AS ?", &[path.as_ref(), db_name])?);
        }

        Ok(())
    }

    fn with_connection<F, T>(&self, db_name: &str, mut f: F) -> PrismaResult<T>
    where
        F: FnMut(Connection) -> PrismaResult<T>,
    {
        let mut conn = dbg!(self.pool.get()?);
        Self::find_or_create_database(&mut conn, db_name)?;
        f(conn)
    }
}

impl Connector for Sqlite {
    fn select_1(&self) -> PrismaResult<i32> {
        let conn = self.pool.get()?;
        let mut stmt = conn.prepare("SELECT 1")?;
        let mut rows = stmt.query_map(NO_PARAMS, |row| row.get(0))?;

        match rows.next() {
            Some(r) => Ok(r?),
            None => Err(Error::NoResultsError),
        }
    }

    fn get_node_by_where(
        &self,
        project: &Project,
        selector: &NodeSelector,
    ) -> PrismaResult<String> {
        self.with_connection(&project.db_name(), |conn| {
            let mut stmt = conn.prepare("SELECT * FROM :table WHERE :field = :value")?;

            let params = vec![
                (":table", (&selector.table as &ToSql)),
                (":field", (&selector.field as &ToSql)),
                (":value", (selector.value as &ToSql)),
            ];

            stmt.query_map_named(&params, |_| String::from("TODO"))?;

            Ok(String::from("TODO"))
        })
    }
}
