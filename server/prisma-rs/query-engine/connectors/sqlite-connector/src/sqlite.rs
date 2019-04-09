mod executor;
mod mutaction_executor;
mod read;
mod resolver;
mod write;

use chrono::{DateTime, Utc};
use connector::*;
use prisma_models::prelude::*;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{Row, Transaction, NO_PARAMS};
use std::{collections::HashSet, env};
use uuid::Uuid;

type Connection = r2d2::PooledConnection<SqliteConnectionManager>;
type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    pool: Pool,
    test_mode: bool,
}

impl Sqlite {
    /// Creates a new SQLite pool connected into local memory.
    pub fn new(connection_limit: u32, test_mode: bool) -> ConnectorResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        Ok(Sqlite { pool, test_mode })
    }

    /// When querying and we haven't yet loaded the database, it'll be loaded on
    /// or created to `$SERVER_ROOT/db/{db_name}.db`.
    ///
    /// The database is then attached to the memory with an alias of `{db_name}`.
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

        dbg!(conn.execute("PRAGMA foreign_keys = ON", NO_PARAMS)?);

        Ok(())
    }

    /// If querying a single integer, such as a `COUNT()`, the function will get
    /// the first column with the default value being `0`.
    pub fn fetch_int(row: &Row) -> i64 {
        row.get_checked(0).unwrap_or(0)
    }

    /// Read and cast a `Row` into a `Record`, casting the columns from the
    /// `DataModel` definitions.
    pub fn read_row(row: &Row, selected_fields: &SelectedFields) -> ConnectorResult<Node> {
        let mut fields = Vec::new();

        for (i, typid) in selected_fields.type_identifiers().iter().enumerate() {
            fields.push(Self::fetch_value(*typid, &row, i)?);
        }

        Ok(Node::new(fields))
    }

    /// Converter function to wrap the limited set of types in SQLite to the internal `PrismaValue`
    /// definition.
    pub fn fetch_value(typ: TypeIdentifier, row: &Row, i: usize) -> ConnectorResult<PrismaValue> {
        let result = match typ {
            TypeIdentifier::String => row.get_checked(i).map(|val| PrismaValue::String(val)),
            TypeIdentifier::GraphQLID => row.get_checked(i).map(|val| PrismaValue::GraphqlId(val)),
            TypeIdentifier::UUID => {
                let result: Result<String, rusqlite::Error> = row.get_checked(i);

                if let Ok(val) = result {
                    let uuid = Uuid::parse_str(val.as_ref())?;
                    Ok(PrismaValue::Uuid(uuid))
                } else {
                    result.map(|s| PrismaValue::String(s))
                }
            }
            TypeIdentifier::Int => row.get_checked(i).map(|val| PrismaValue::Int(val)),
            TypeIdentifier::Boolean => row.get_checked(i).map(|val| PrismaValue::Boolean(val)),
            TypeIdentifier::Enum => row.get_checked(i).map(|val| PrismaValue::Enum(val)),
            TypeIdentifier::Json => row.get_checked(i).map(|val| PrismaValue::Json(val)),
            TypeIdentifier::DateTime => row.get_checked(i).map(|ts: i64| {
                let nsecs = ((ts % 1000) * 1_000_000) as u32;
                let secs = (ts / 1000) as i64;
                let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
                let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

                PrismaValue::DateTime(datetime)
            }),
            TypeIdentifier::Relation => row.get_checked(i).map(|val| PrismaValue::GraphqlId(val)),
            TypeIdentifier::Float => row.get_checked(i).map(|val: f64| PrismaValue::Float(val)),
        };

        match result {
            Err(rusqlite::Error::InvalidColumnType(_, rusqlite::types::Type::Null)) => Ok(PrismaValue::Null),
            Ok(pv) => Ok(pv),
            Err(e) => Err(e.into()),
        }
    }

    /// Takes a new connection and if needed attaches the database if needed.
    ///
    /// [with_transaction](struct.Sqlite.html#method.with_transaction) should be
    /// used if atomicity is needed.
    pub fn with_connection<F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&mut Connection) -> ConnectorResult<T>,
    {
        let mut conn = self.pool.get()?;
        Self::attach_database(&mut conn, db_name)?;

        let result = f(&mut conn);

        if self.test_mode {
            dbg!(conn.execute("DETACH DATABASE ?", &[db_name])?);
        }

        result
    }

    /// Takes a new connection and starts a transaction, that is commited if the
    /// given `F` was successful. Attaches any databases, if not yet in memory.
    ///
    /// [with_connection](struct.Sqlite.html#methid.with_connection) should be
    /// used if atomicity is not needed.
    /// ```rust
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use connector::{error::ConnectorError, ConnectorResult};
    /// # use sqlite_connector::*;
    /// # use prisma_query::ast::*;
    /// # let sqlite = Sqlite::new(1, false).unwrap();
    /// let _ = sqlite.with_transaction("test", |trans| {
    ///     trans.execute(
    ///         "CREATE TABLE IF NOT EXISTS test.users (id Text, name Text);",
    ///         NO_PARAMS
    ///     ).unwrap();
    ///
    ///     Ok(())
    /// });
    ///
    /// let _: ConnectorResult<()> = sqlite.with_transaction("test", |trans| {
    ///     trans.execute(
    ///         "INSERT INTO test.users (id, name) VALUES ('id1', 'John')",
    ///         NO_PARAMS,
    ///     ).unwrap();
    ///
    ///     Err(ConnectorError::RelationViolation {
    ///         relation_name: String::from("Cats"),
    ///         model_a_name: String::from("A"),
    ///         model_b_name: String::from("B"),
    ///     })
    /// });
    ///
    /// let count: i64 = sqlite.with_connection("test", |conn| {
    ///     let res = conn.query_row("SELECT COUNT(id) FROM test.users", NO_PARAMS, |row| {
    ///         row.get_checked(0).unwrap_or(0)
    ///     })?;
    ///
    ///     Ok(res)
    /// }).unwrap();
    ///
    /// assert_eq!(0, count);
    /// ```
    pub fn with_transaction<F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&Transaction) -> ConnectorResult<T>,
    {
        self.with_connection(db_name, |conn| {
            let tx = conn.transaction()?;
            let result = f(&tx);

            if result.is_ok() {
                tx.commit()?;
            }

            result
        })
    }
}
