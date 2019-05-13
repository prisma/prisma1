use crate::{MutationBuilder, SqlId, SqlResult, SqlRow, ToSqlRow, Transaction, Transactional};
use chrono::{DateTime, Utc};
use prisma_models::{GraphqlId, PrismaValue, ProjectRef, TypeIdentifier};
use prisma_query::{
    ast::{Query, Select},
    visitor::{self, Visitor},
};
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{
    types::{FromSql, FromSqlResult, Type as SqliteType, ValueRef},
    Connection, Error as SqliteError, Row as SqliteRow, Transaction as SqliteTransaction, NO_PARAMS,
};
use std::collections::HashSet;
use uuid::Uuid;

type Pool = r2d2::Pool<SqliteConnectionManager>;

/// SQLite is a C-language library that implements a small, fast,
/// self-contained, high-reliability, full-featured, SQL database engine.
pub struct Sqlite {
    databases_folder_path: String,
    pool: Pool,
    test_mode: bool,
}

impl Transactional for Sqlite {
    fn with_transaction<F, T>(&self, db: &str, f: F) -> SqlResult<T>
    where
        F: FnOnce(&mut Transaction) -> SqlResult<T>,
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
    fn write(&mut self, q: Query) -> SqlResult<Option<GraphqlId>> {
        let (sql, params) = dbg!(visitor::Sqlite::build(q));

        let mut stmt = self.prepare_cached(&sql)?;
        stmt.execute(params)?;

        Ok(Some(GraphqlId::Int(self.last_insert_rowid() as usize)))
    }

    fn filter(&mut self, q: Select, idents: &[TypeIdentifier]) -> SqlResult<Vec<SqlRow>> {
        let (sql, params) = dbg!(visitor::Sqlite::build(q));

        let mut stmt = self.prepare_cached(&sql)?;
        let mut rows = stmt.query(params)?;
        let mut result = Vec::new();

        while let Some(row) = rows.next() {
            result.push(row?.to_prisma_row(idents)?);
        }

        Ok(result)
    }

    fn truncate(&mut self, project: ProjectRef) -> SqlResult<()> {
        self.write(Query::from("PRAGMA foreign_keys = OFF"))?;

        for delete in MutationBuilder::truncate_tables(project) {
            self.delete(delete)?;
        }

        self.write(Query::from("PRAGMA foreign_keys = ON"))?;

        Ok(())
    }
}

impl FromSql for SqlId {
    fn column_result(value: ValueRef<'_>) -> FromSqlResult<Self> {
        value
            .as_str()
            .and_then(|strval| {
                let res = Uuid::from_slice(strval.as_bytes())
                    .map(|uuid| SqlId::UUID(uuid))
                    .unwrap_or_else(|_| SqlId::String(strval.to_string()));

                Ok(res)
            })
            .or_else(|_| value.as_i64().map(|intval| SqlId::Int(intval as usize)))
    }
}

impl<'a, 'stmt> ToSqlRow for SqliteRow<'a, 'stmt> {
    fn to_prisma_row<'b, T>(&'b self, idents: T) -> SqlResult<SqlRow>
    where
        T: IntoIterator<Item = &'b TypeIdentifier>,
    {
        fn convert(row: &SqliteRow, i: usize, typid: &TypeIdentifier) -> SqlResult<PrismaValue> {
            let result = match typid {
                TypeIdentifier::String => row.get_checked(i).map(|val| PrismaValue::String(val)),
                TypeIdentifier::GraphQLID | TypeIdentifier::Relation => row.get_checked(i).map(|val| {
                    let id: SqlId = val;
                    PrismaValue::GraphqlId(GraphqlId::from(id))
                }),
                TypeIdentifier::Float => row.get_checked(i).map(|val| PrismaValue::Float(val)),
                TypeIdentifier::Int => row.get_checked(i).map(|val| PrismaValue::Int(val)),
                TypeIdentifier::Boolean => row.get_checked(i).map(|val| PrismaValue::Boolean(val)),
                TypeIdentifier::Enum => row.get_checked(i).map(|val| PrismaValue::Enum(val)),
                TypeIdentifier::Json => row.get_checked(i).and_then(|val| {
                    let val: String = val;
                    serde_json::from_str(&val).map(|r| PrismaValue::Json(r)).map_err(|err| {
                        SqliteError::FromSqlConversionFailure(i as usize, SqliteType::Text, Box::new(err))
                    })
                }),
                TypeIdentifier::UUID => {
                    let result: Result<String, _> = row.get_checked(i);

                    if let Ok(val) = result {
                        let uuid = Uuid::parse_str(val.as_ref())?;

                        Ok(PrismaValue::Uuid(uuid))
                    } else {
                        result.map(|s| PrismaValue::String(s))
                    }
                }
                TypeIdentifier::DateTime => row.get_checked(i).map(|ts: i64| {
                    let nsecs = ((ts % 1000) * 1_000_000) as u32;
                    let secs = (ts / 1000) as i64;
                    let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
                    let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

                    PrismaValue::DateTime(datetime)
                }),
            };

            match result {
                Ok(pv) => Ok(pv),
                Err(rusqlite::Error::InvalidColumnType(_, rusqlite::types::Type::Null)) => Ok(PrismaValue::Null),
                Err(e) => Err(e.into()),
            }
        }

        let mut row = SqlRow::default();

        for (i, typid) in idents.into_iter().enumerate() {
            row.values.push(convert(self, i, typid)?);
        }

        Ok(row)
    }
}

impl Sqlite {
    /// Creates a new SQLite pool connected into local memory.
    pub fn new(databases_folder_path: String, connection_limit: u32, test_mode: bool) -> SqlResult<Sqlite> {
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
    fn attach_database(&self, conn: &mut Connection, db_name: &str) -> SqlResult<()> {
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

    fn with_connection<F, T>(&self, db: &str, f: F) -> SqlResult<T>
    where
        F: FnOnce(&mut Connection) -> SqlResult<T>,
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
