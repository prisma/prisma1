use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{types::ToSql, NO_PARAMS};

use std::{collections::HashSet, sync::Arc};
use arc_swap::ArcSwap;

use crate::{
    connector::Connector, error::Error, project::Project, querying::NodeSelector, PrismaResult,
};

type Connection = r2d2::PooledConnection<SqliteConnectionManager>;
type Databases = ArcSwap<HashSet<String>>;
type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    pool: Pool,
    databases: Databases,
}

impl Sqlite {
    pub fn new(connection_limit: u32) -> PrismaResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        let mut conn = pool.get()?;
        let databases = ArcSwap::from(Arc::new(HashSet::new()));
        let this = Sqlite { pool, databases };
        
        this.update_databases(&mut conn)?;

        Ok(this)
    }

    /// Updates the set of existing databases for caching purposes.
    fn update_databases(&self, conn: &mut Connection) -> PrismaResult<()> {
        let mut stmt = conn.prepare("PRAGMA database_list")?;

        let databases: HashSet<String> = stmt
            .query_map(NO_PARAMS, |row| {
                let name: String = row.get(1);
                name
            })?
            .map(|res| res.unwrap())
            .collect();

        self.databases.store(Arc::new(databases));

        Ok(())
    }

    fn has_database(&self, db_name: &str) -> bool {
        self.databases.load().contains(db_name)
    }

    fn create_database(conn: &mut Connection, db_name: &str) -> PrismaResult<()> {
        let path = format!("db/{}", db_name);
        dbg!(conn.execute("ATTACH DATABASE ? AS ?", &[path.as_ref(), db_name])?);

        Ok(())
    }

    /// Take a new connection from the pool and create the database if it
    /// doesn't exist yet.
    fn with_connection<F, T>(&self, db_name: &str, mut f: F) -> PrismaResult<T>
    where
        F: FnMut(Connection) -> PrismaResult<T>,
    {
        let mut conn = dbg!(self.pool.get()?);

        if !self.has_database(db_name) {
            // We might have a race if having more clients for the same
            // databases. Create will silently fail and we'll get the database
            // name to our bookkeeping.
            Self::create_database(&mut conn, db_name)?;
            self.update_databases(&mut conn)?;
        }

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
