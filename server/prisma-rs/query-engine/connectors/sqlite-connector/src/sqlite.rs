use crate::DatabaseExecutor;
use connector::*;
use prisma_common::PrismaResult;
use prisma_query::{
    ast::*,
    visitor::{self, *},
};
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{Row, NO_PARAMS};
use serde_json::Value;
use std::{collections::HashSet, env};

type Connection = r2d2::PooledConnection<SqliteConnectionManager>;
type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    pool: Pool,
    test_mode: bool,
}

impl DatabaseExecutor for Sqlite {
    fn with_rows<F, T>(&self, query: Select, db_name: String, mut f: F) -> PrismaResult<Vec<T>>
    where
        F: FnMut(&Row) -> PrismaResult<T>,
    {
        self.with_connection(&db_name, |conn| {
            let (query_sql, params) = dbg!(visitor::Sqlite::build(query));

            let res: PrismaResult<Vec<T>> = conn
                .prepare(&query_sql)?
                .query_map(&params, |row| f(row))?
                .map(|row_res| row_res.unwrap())
                .collect();

            Ok(res?)
        })
    }
}

impl DatabaseMutactionExecutor for Sqlite {
    fn execute_raw(&self, _query: String) -> PrismaResult<Value> {
        // self.sqlite.with_connection(&db_name, |conn| {
        //     let res = conn
        //         .prepare(&query)?
        //         .query_map(&params, |row| f(row))?
        //         .map(|row_res| row_res.unwrap())
        //         .collect();

        //     Ok(res)
        // });
        Ok(Value::String("hello world!".to_string()))
    }

    fn execute(&self, _db_name: String, _mutaction: DatabaseMutaction) -> PrismaResult<DatabaseMutactionResults> {
        unimplemented!()
        /*
         *
        self.with_connection(&db_name, |conn| {
            let tx = conn.transaction()?;
            let plan = MutactionPlan::from(mutaction);

            let id = plan.steps.into_iter().fold(None, |acc, step| {
                let query = match self.needing {
                    Some(returning) => match &*returning.read() {
                        Returning::Expected => panic!("Needed ID value not set for mutaction"),
                        Returning::Got(id) => match self.query {
                            Query::Insert(insert) => Query::Insert(insert.value(id)),
                            _ => panic!("Only inserts are supported for now"),
                        },
                    },
                    None => query,
                };

                let id: Option<GraphqlId> = executor.with_rows(self.query, db_name, |row| row.get(0));

                if let Some(returning) = self.returning {
                    returning.write().set(id);
                };
            });

            DatabaseMutactionResult(id, mutaction);

            tx.commit()?;
        })
         */
    }
}

impl Sqlite {
    /// Creates a new SQLite pool connected into local memory. By querying from
    /// different databases, it will try to create them to
    /// `$SERVER_ROOT/db/db_name` if they do not exists yet.
    pub fn new(connection_limit: u32, test_mode: bool) -> PrismaResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        Ok(Sqlite { pool, test_mode })
    }

    /// Will create a new file if it doesn't exist. Otherwise loads db/db_name
    /// from the SERVER_ROOT.
    fn attach_database(conn: &mut Connection, db_name: &str) -> PrismaResult<()> {
        let mut stmt = dbg!(conn.prepare("PRAGMA database_list")?);

        let databases: HashSet<String> = stmt
            .query_map(NO_PARAMS, |row| {
                let name: String = row.get(1);
                name
            })?
            .map(|res| res.unwrap())
            .collect();

        // FIXME(Dom): Correct config for sqlite
        let server_root = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
        if !databases.contains(db_name) {
            let path = format!("{}/db/{}.db", server_root, db_name);
            dbg!(conn.execute("ATTACH DATABASE ? AS ?", &[path.as_ref(), db_name])?);
        }

        Ok(())
    }

    /// Take a new connection from the pool and create the database if it
    /// doesn't exist yet.
    pub fn with_connection<F, T>(&self, db_name: &str, f: F) -> PrismaResult<T>
    where
        F: FnOnce(&Connection) -> PrismaResult<T>,
    {
        let mut conn = dbg!(self.pool.get()?);
        Self::attach_database(&mut conn, db_name)?;

        let result = f(&conn);

        if self.test_mode {
            dbg!(conn.execute("DETACH DATABASE ?", &[db_name])?);
        }

        result
    }
}
