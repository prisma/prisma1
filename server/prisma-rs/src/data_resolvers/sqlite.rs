mod data_resolver;
mod service;

use chrono::{DateTime, Utc};
use r2d2_sqlite::SqliteConnectionManager;
use std::collections::HashSet;

use crate::{models::prelude::*, protobuf::prelude::*, PrismaResult, PrismaValue, SERVER_ROOT};

use rusqlite::{
    types::{FromSql, FromSqlResult, Null, ToSql, ToSqlOutput, ValueRef},
    Error as RusqlError, Row, NO_PARAMS,
};

type Connection = r2d2::PooledConnection<SqliteConnectionManager>;
type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    pool: Pool,
    test_mode: bool,
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
    fn create_database(conn: &mut Connection, db_name: &str) -> PrismaResult<()> {
        let mut stmt = dbg!(conn.prepare("PRAGMA database_list")?);

        let databases: HashSet<String> = stmt
            .query_map(NO_PARAMS, |row| {
                let name: String = row.get(1);
                name
            })?
            .map(|res| res.unwrap())
            .collect();

        if !databases.contains(db_name) {
            let path = format!("{}/db/{}", *SERVER_ROOT, db_name);
            dbg!(conn.execute("ATTACH DATABASE ? AS ?", &[path.as_ref(), db_name])?);
        }

        Ok(())
    }

    /// Take a new connection from the pool and create the database if it
    /// doesn't exist yet.
    fn with_connection<F, T>(&self, db_name: &str, f: F) -> PrismaResult<T>
    where
        F: FnOnce(&Connection) -> PrismaResult<T>,
    {
        let mut conn = dbg!(self.pool.get()?);
        Self::create_database(&mut conn, db_name)?;

        let result = f(&conn);

        if self.test_mode {
            dbg!(conn.execute("DETACH DATABASE ?", &[db_name])?);
        }

        result
    }

    /// Converter function to wrap the limited set of types in SQLite to a
    /// richer PrismaValue.
    fn fetch_value(typ: TypeIdentifier, row: &Row, i: usize) -> PrismaValue {
        match typ {
            TypeIdentifier::String => PrismaValue::String(row.get(i)),
            TypeIdentifier::GraphQLID => PrismaValue::GraphqlId(row.get(i)),
            TypeIdentifier::UUID => PrismaValue::Uuid(row.get(i)),
            TypeIdentifier::Int => PrismaValue::Int(row.get(i)),
            TypeIdentifier::Boolean => PrismaValue::Boolean(row.get(i)),
            TypeIdentifier::Enum => PrismaValue::Enum(row.get(i)),
            TypeIdentifier::Json => PrismaValue::Json(row.get(i)),
            TypeIdentifier::DateTime => {
                let ts: i64 = row.get(i);
                let nsecs = ((ts % 1000) * 1_000_000) as u32;
                let secs = (ts / 1000) as i64;
                let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
                let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

                PrismaValue::DateTime(datetime.to_rfc3339())
            }
            TypeIdentifier::Relation => panic!("We should not have a Relation here!"),
            TypeIdentifier::Float => {
                let v: f64 = row.get(i);
                PrismaValue::Float(v as f32)
            }
        }
    }

    /// Helper to namespace different databases.
    fn table_location(database: &str, table: &str) -> String {
        format!("{}.{}", database, table)
    }
}

impl ToSql for PrismaValue {
    fn to_sql(&self) -> Result<ToSqlOutput, RusqlError> {
        let value = match self {
            PrismaValue::String(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Enum(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Json(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Uuid(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Float(value) => ToSqlOutput::from(f64::from(*value)),
            PrismaValue::Int(value) => ToSqlOutput::from(*value),
            PrismaValue::Boolean(value) => ToSqlOutput::from(*value),
            PrismaValue::DateTime(value) => value.to_sql().unwrap(),
            PrismaValue::Null(_) => ToSqlOutput::from(Null),

            PrismaValue::GraphqlId(value) => match value.id_value {
                Some(graphql_id::IdValue::String(ref value)) => {
                    ToSqlOutput::from(value.as_ref() as &str)
                }
                Some(graphql_id::IdValue::Int(value)) => ToSqlOutput::from(value),
                None => panic!("We got an empty ID value here. Tsk tsk."),
            },

            PrismaValue::Relation(_) => panic!("We should not have a Relation value here."),
        };

        Ok(value)
    }
}

impl FromSql for GraphqlId {
    fn column_result(value: ValueRef<'_>) -> FromSqlResult<Self> {
        value
            .as_str()
            .map(|strval| GraphqlId {
                id_value: Some(graphql_id::IdValue::String(strval.to_string())),
            })
            .or_else(|_| {
                value.as_i64().map(|intval| GraphqlId {
                    id_value: Some(graphql_id::IdValue::Int(intval)),
                })
            })
    }
}
