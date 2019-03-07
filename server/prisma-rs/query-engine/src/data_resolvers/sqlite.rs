mod data_resolver;
mod service;

use crate::data_resolvers::SelectedFields;
use chrono::{DateTime, Utc};
use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use prisma_query::ast::*;
use r2d2_sqlite::SqliteConnectionManager;
use std::collections::HashSet;

use crate::SERVER_ROOT;

use rusqlite::{Row, NO_PARAMS};

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
            let path = format!("{}/db/{}.db", *SERVER_ROOT, db_name);
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
        let result = match typ {
            TypeIdentifier::String => row.get_checked(i).map(|val| PrismaValue::String(val)),
            TypeIdentifier::GraphQLID => row.get_checked(i).map(|val| PrismaValue::GraphqlId(val)),
            TypeIdentifier::UUID => row.get_checked(i).map(|val| PrismaValue::Uuid(val)),
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
            TypeIdentifier::Relation => panic!("We should not have a Relation here!"),
            TypeIdentifier::Float => row.get_checked(i).map(|val: f64| PrismaValue::Float(val)),
        };

        result.unwrap_or_else(|e| match e {
            rusqlite::Error::InvalidColumnType(_, rusqlite::types::Type::Null) => PrismaValue::Null,
            _ => panic!(e),
        })
    }

    fn base_query(table: &str, conditions: ConditionTree, offset: usize) -> Select {
        Select::from(table).so_that(conditions).offset(offset)
    }

    fn select_fields(select: Select, fields: &SelectedFields) -> Select {
        fields
            .names_of_scalar_non_list_fields()
            .iter()
            .fold(select, |acc, field| acc.column(field.as_ref()))
    }
}
