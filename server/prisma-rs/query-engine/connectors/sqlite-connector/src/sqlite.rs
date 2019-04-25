mod mutaction_executor;
mod read;
mod resolver;
mod write;

use crate::{Connection, TransactionalExecutor};
use chrono::{DateTime, Utc};
use connector::*;
use prisma_models::prelude::*;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{Row, Transaction, NO_PARAMS};
use std::collections::HashSet;
use uuid::Uuid;

type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    databases_folder_path: String,
    pool: Pool,
    test_mode: bool,
}

impl TransactionalExecutor for Sqlite {
    fn with_connection<'a, F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&mut Connection) -> ConnectorResult<T>,
    {
        let mut conn = self.pool.get()?;
        self.attach_database(&mut conn, db_name)?;

        let result = f(&mut conn);
        if self.test_mode {
            conn.execute("DETACH DATABASE ?", &[db_name])?;
        }

        result
    }

    fn with_transaction<F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&Transaction) -> ConnectorResult<T>,
    {
        self.with_connection(db_name, |conn| {
            let tx = conn.transaction()?;
            tx.set_prepared_statement_cache_capacity(65536);

            let result = f(&tx);

            if result.is_ok() {
                tx.commit()?;
            }

            result
        })
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

    pub fn without_foreign_key_checks<F, T>(conn: &Transaction, f: F) -> ConnectorResult<T>
    where
        F: FnOnce() -> ConnectorResult<T>,
    {
        conn.execute("PRAGMA foreign_keys = OFF", NO_PARAMS)?;
        let res = f()?;
        conn.execute("PRAGMA foreign_keys = ON", NO_PARAMS)?;
        Ok(res)
    }

    /// If querying a single integer, such as a `COUNT()`, the function will get
    /// the first column with the default value being `0`.
    pub fn fetch_int(row: &Row) -> i64 {
        row.get_checked(0).unwrap_or(0)
    }

    pub fn fetch_id(row: &Row) -> ConnectorResult<GraphqlId> {
        Ok(row.get_checked(0)?)
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
}
