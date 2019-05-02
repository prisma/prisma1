use crate::*;
use connector::*;
use prisma_models::{ProjectRef, TypeIdentifier};
use prisma_query::{
    ast::{Query, Select},
    visitor::{self, Visitor},
};
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{Connection, Transaction as SqliteTransaction, NO_PARAMS};
use std::collections::HashSet;

type Pool = r2d2::Pool<SqliteConnectionManager>;

/// SQLite is a C-language library that implements a small, fast,
/// self-contained, high-reliability, full-featured, SQL database engine.
pub struct Sqlite {
    databases_folder_path: String,
    pool: Pool,
    test_mode: bool,
}

impl Transactional for Sqlite {
    fn with_transaction<F, T>(&self, db: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&mut Transaction) -> ConnectorResult<T>,
    {
        self.with_connection(db, |ref mut conn| {
            let mut tx = conn.transaction()?;
            tx.set_prepared_statement_cache_capacity(65536);

            let result = f(&mut tx);

            if result.is_ok() {
                tx.commit()?;
            }

            result
        })
    }
}

impl<'a> Transaction for SqliteTransaction<'a> {
    fn write(&mut self, q: Query) -> ConnectorResult<WriteItems> {
        let (sql, params) = dbg!(visitor::Sqlite::build(q));
        let mut stmt = self.prepare_cached(&sql)?;

        Ok(WriteItems {
            count: stmt.execute(params)? as usize,
            last_id: self.last_insert_rowid() as usize,
        })
    }

    fn filter(&mut self, q: Select, idents: &[TypeIdentifier]) -> ConnectorResult<Vec<PrismaRow>> {
        let (sql, params) = dbg!(visitor::Sqlite::build(q));

        let mut stmt = self.prepare_cached(&sql)?;
        let mut rows = stmt.query(params)?;
        let mut result = Vec::new();

        while let Some(row) = rows.next() {
            result.push(row?.to_prisma_row(idents)?);
        }

        Ok(result)
    }

    fn truncate(&mut self, project: ProjectRef) -> ConnectorResult<()> {
        self.write(Query::from("PRAGMA foreign_keys = OFF"))?;

        for delete in MutationBuilder::truncate_tables(project) {
            self.delete(delete)?;
        }

        self.write(Query::from("PRAGMA foreign_keys = ON"))?;

        Ok(())
    }
}

impl Sqlite {
    /// Creates a new SQLite pool connected into local memory.
    pub fn new(databases_folder_path: String, connection_limit: u32, test_mode: bool) -> ConnectorResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        Ok(Sqlite {
            databases_folder_path,
            pool,
            test_mode,
        })
    }

    /// When querying and we haven't yet loaded the database, it'll be loaded on
    /// or created to the configured database file.
    ///
    /// The database is then attached to the memory with an alias of `{db_name}`.
    fn attach_database(&self, conn: &mut Connection, db_name: &str) -> ConnectorResult<()> {
        let mut stmt = conn.prepare("PRAGMA database_list")?;

        let databases: HashSet<String> = stmt
            .query_map(NO_PARAMS, |row| {
                let name: String = row.get(1);
                name
            })?
            .map(|res| res.unwrap())
            .collect();

        if !databases.contains(db_name) {
            // This is basically hacked until we have a full rust stack with a migration engine.
            // Currently, the scala tests use the JNA library to write to the database. This
            let database_file_path = format!("{}/{}.db", self.databases_folder_path, db_name);
            conn.execute("ATTACH DATABASE ? AS ?", &[database_file_path.as_ref(), db_name])?;
        }

        conn.execute("PRAGMA foreign_keys = ON", NO_PARAMS)?;
        Ok(())
    }

    fn with_connection<F, T>(&self, db: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&mut Connection) -> ConnectorResult<T>,
    {
        let mut conn = self.pool.get()?;
        self.attach_database(&mut conn, db)?;

        let result = f(&mut conn);

        if self.test_mode {
            conn.execute("DETACH DATABASE ?", &[db])?;
        }

        result
    }
}
