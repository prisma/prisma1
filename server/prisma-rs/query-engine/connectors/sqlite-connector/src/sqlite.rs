use crate::{DatabaseExecutor, MutactionPlan, Returning};
use connector::*;
use prisma_common::PrismaResult;
use prisma_models::prelude::*;
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

    fn execute(&self, db_name: String, mutaction: DatabaseMutaction) -> PrismaResult<DatabaseMutactionResults> {
        let plan = MutactionPlan::from(mutaction);
        let mut results = DatabaseMutactionResults::default();

        self.with_connection(&db_name, |ref mut conn| {
            let tx = conn.transaction()?;
            let mut mutaction_id = None;

            for mut step in plan.steps.into_iter() {
                // REFACTOR HUNT STARTS
                if let Some((column, needing)) = step.needing.clone() {
                    if let Returning::Got(id) = &*needing.read() {
                        if let Query::Insert(insert) = step.query {
                            step.query = Query::from(insert.value(column, id.clone()));
                        }
                    }
                };
                // REFACTOR HUNT ENDS

                let (sql, params) = visitor::Sqlite::build(step.query);
                tx.prepare(&sql)?.execute(&params)?;

                if let Some((id_column, returning)) = step.returning {
                    let ast = Select::from(step.table.clone())
                        .column(id_column)
                        .so_that("row_id".equals(tx.last_insert_rowid()));

                    let (sql, params) = visitor::Sqlite::build(ast);

                    let id: GraphqlId = tx
                        .prepare(&sql)?
                        .query_map(&params, |row| row.get(0))?
                        .map(|row_res| row_res.unwrap())
                        .next()
                        .unwrap();

                    let mut switch = returning.write();
                    *switch = Returning::Got(id.clone());

                    mutaction_id = Some(id);
                };
            }

            results.push(DatabaseMutactionResult {
                id: mutaction_id.unwrap(),
                typ: plan.mutaction.typ(),
                mutaction: plan.mutaction,
            });

            tx.commit()?;

            Ok(results)
        })
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
            let path = dbg!(format!("{}/db/{}.db", server_root, db_name));
            dbg!(conn.execute("ATTACH DATABASE ? AS ?", &[path.as_ref(), db_name])?);
        }

        Ok(())
    }

    /// Take a new connection from the pool and create the database if it
    /// doesn't exist yet.
    pub fn with_connection<F, T>(&self, db_name: &str, f: F) -> PrismaResult<T>
    where
        F: FnOnce(&mut Connection) -> PrismaResult<T>,
    {
        let mut conn = dbg!(self.pool.get()?);
        Self::attach_database(&mut conn, db_name)?;

        let result = f(&mut conn);

        if self.test_mode {
            dbg!(conn.execute("DETACH DATABASE ?", &[db_name])?);
        }

        result
    }
}
