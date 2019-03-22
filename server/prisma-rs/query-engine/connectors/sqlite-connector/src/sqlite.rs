use crate::DatabaseExecutor;
use connector::*;
use prisma_models::prelude::*;
use prisma_query::{
    ast::*,
    visitor::{self, *},
};
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{Row, Transaction, NO_PARAMS};
use serde_json::Value;
use std::{collections::HashSet, env};

type Connection = r2d2::PooledConnection<SqliteConnectionManager>;
type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    pool: Pool,
    test_mode: bool,
}

impl DatabaseExecutor for Sqlite {
    fn with_rows<F, T>(&self, query: Select, db_name: String, mut f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
    {
        self.with_transaction(&db_name, |conn| {
            let (query_sql, params) = dbg!(visitor::Sqlite::build(query));

            let res: ConnectorResult<Vec<T>> = conn
                .prepare(&query_sql)?
                .query_map(&params, |row| f(row))?
                .map(|row_res| row_res.unwrap())
                .collect();

            Ok(res?)
        })
    }
}

impl DatabaseMutactionExecutor for Sqlite {
    fn execute_raw(&self, _query: String) -> ConnectorResult<Value> {
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

    fn execute(&self, db_name: String, mutaction: DatabaseMutaction) -> ConnectorResult<DatabaseMutactionResults> {
        let mut results = DatabaseMutactionResults::default();

        self.with_transaction(&db_name, |conn| {
            let results = match mutaction {
                DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::CreateNode(ref x)) => {
                    let (insert, returned_id) = MutationBuilder::create_node(x.model.clone(), x.non_list_args.clone());
                    let (sql, params) = dbg!(visitor::Sqlite::build(insert));

                    conn.prepare(&sql)?.execute(&params)?;

                    let id = match returned_id {
                        Some(id) => id,
                        None => GraphqlId::Int(conn.last_insert_rowid() as usize),
                    };

                    for (field_name, list_value) in x.list_args.clone() {
                        let field = x.model.fields().find_from_scalar(&field_name).unwrap();
                        let table = field.scalar_list_table();
                        let inserts = MutationBuilder::create_scalar_list_value(table.clone(), list_value, id.clone());

                        for insert in inserts {
                            let (sql, params) = dbg!(visitor::Sqlite::build(insert));

                            conn.prepare(&sql)?.execute(&params)?;
                        }
                    }

                    let result = DatabaseMutactionResult {
                        id: id,
                        typ: DatabaseMutactionResultType::Create,
                        mutaction: mutaction,
                    };
                    results.push(result);
                    Ok(results)
                }
                DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::UpdateNode(_)) => unimplemented!(),
                DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::UpsertNode(_)) => unimplemented!(),
                DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::DeleteNode(_)) => unimplemented!(),
                DatabaseMutaction::Nested(_) => panic!("nested mutactions are not supported yet!"),
            };

            results
        })
    }
}

impl Sqlite {
    /// Creates a new SQLite pool connected into local memory. By querying from
    /// different databases, it will try to create them to
    /// `$SERVER_ROOT/db/db_name` if they do not exists yet.
    pub fn new(connection_limit: u32, test_mode: bool) -> ConnectorResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        Ok(Sqlite { pool, test_mode })
    }

    /// Will create a new file if it doesn't exist. Otherwise loads db/db_name
    /// from the SERVER_ROOT.
    fn attach_database(conn: &mut Connection, db_name: &str) -> ConnectorResult<()> {
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

    pub fn with_connection<F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&mut Connection) -> ConnectorResult<T>,
    {
        let mut conn = dbg!(self.pool.get()?);
        Self::attach_database(&mut conn, db_name)?;

        let result = f(&mut conn);

        if self.test_mode {
            dbg!(conn.execute("DETACH DATABASE ?", &[db_name])?);
        }

        result
    }

    /// Take a new connection from the pool and create the database if it
    /// doesn't exist yet.
    pub fn with_transaction<F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&Transaction) -> ConnectorResult<T>,
    {
        self.with_connection(db_name, |conn| {
            let tx = conn.transaction()?;
            let result = f(&tx);

            tx.commit()?;
            result
        })
    }
}
