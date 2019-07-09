use crate::{
    error::SqlError,
    query_builder::{read::ManyRelatedRecordsWithRowNumber, write::WriteQueryBuilder},
    RawQuery, SqlId, SqlResult, SqlRow, ToSqlRow, Transaction, Transactional,
};
use chrono::{DateTime, NaiveDateTime, Utc};
use datamodel::configuration::Source;
use prisma_models::{GraphqlId, InternalDataModelRef, PrismaValue, TypeIdentifier, EnumValue};
use prisma_query::{
    ast::Query,
    visitor::{self, Visitor},
};
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{
    types::{FromSql, FromSqlResult, Type as SqliteType, ValueRef},
    Connection, Error as SqliteError, Row as SqliteRow, Transaction as SqliteTransaction, NO_PARAMS,
};
use serde_json::{Map, Number, Value};
use std::{collections::HashSet, convert::TryFrom, path::PathBuf};
use uuid::Uuid;

type Pool = r2d2::Pool<SqliteConnectionManager>;

/// SQLite is a C-language library that implements a small, fast,
/// self-contained, high-reliability, full-featured, SQL database engine.
pub struct Sqlite {
    file_path: String,
    pool: Pool,
    test_mode: bool,
}

impl Transactional for Sqlite {
    type ManyRelatedRecordsBuilder = ManyRelatedRecordsWithRowNumber;

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
        let (sql, params) = visitor::Sqlite::build(q);
        debug!("{}\n{:?}", sql, params);

        let mut stmt = self.prepare_cached(&sql)?;

        stmt.execute(params)?;

        Ok(Some(GraphqlId::Int(self.last_insert_rowid() as usize)))
    }

    fn filter(&mut self, q: Query, idents: &[TypeIdentifier]) -> SqlResult<Vec<SqlRow>> {
        let (sql, params) = visitor::Sqlite::build(q);
        debug!("{} (params: {:?})", sql, params);

        let mut stmt = self.prepare_cached(&sql)?;
        let mut rows = stmt.query(params)?;
        let mut result = Vec::new();

        while let Some(row) = rows.next()? {
            result.push(row.to_sql_row(idents)?);
        }

        Ok(result)
    }

    fn truncate(&mut self, internal_data_model: InternalDataModelRef) -> SqlResult<()> {
        self.write(Query::from("PRAGMA foreign_keys = OFF"))?;

        for delete in WriteQueryBuilder::truncate_tables(internal_data_model) {
            self.delete(delete)?;
        }

        self.write(Query::from("PRAGMA foreign_keys = ON"))?;

        Ok(())
    }

    fn raw(&mut self, q: RawQuery) -> SqlResult<Value> {
        let columns: Vec<String> = self
            .prepare_cached(&q.0)?
            .column_names()
            .into_iter()
            .map(ToString::to_string)
            .collect();

        let mut stmt = self.prepare_cached(&q.0)?;

        if q.is_select() {
            let mut rows = stmt.query(NO_PARAMS)?;
            let mut result = Vec::new();

            while let Some(row) = rows.next()? {
                let mut object = Map::new();

                for (i, column) in columns.iter().enumerate() {
                    let value = match row.get_raw(i) {
                        ValueRef::Null => Value::Null,
                        ValueRef::Integer(i) => Value::Number(Number::from(i)),
                        ValueRef::Real(f) => Value::Number(Number::from_f64(f).unwrap()),
                        ValueRef::Text(s) => Value::String(String::from(s)),
                        ValueRef::Blob(b) => Value::String(String::from_utf8(b.to_vec())?),
                    };

                    object.insert(String::from(column.as_str()), value);
                }

                result.push(Value::Object(object));
            }

            Ok(Value::Array(result))
        } else {
            let changes = stmt.execute(NO_PARAMS)?;

            Ok(Value::Number(Number::from(changes)))
        }
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

impl<'a> ToSqlRow for SqliteRow<'a> {
    fn to_sql_row<'b, T>(&'b self, idents: T) -> SqlResult<SqlRow>
    where
        T: IntoIterator<Item = &'b TypeIdentifier>,
    {
        fn convert(row: &SqliteRow, i: usize, typid: &TypeIdentifier) -> SqlResult<PrismaValue> {
            let column = &row.columns()[i];

            let result = match typid {
                TypeIdentifier::String => row.get(i).map(|val| PrismaValue::String(val)),
                TypeIdentifier::GraphQLID | TypeIdentifier::Relation => row.get(i).map(|val| {
                    let id: SqlId = val;
                    PrismaValue::GraphqlId(GraphqlId::from(id))
                }),
                TypeIdentifier::Float => row.get(i).map(|val| PrismaValue::Float(val)),
                TypeIdentifier::Int => row.get(i).map(|val| PrismaValue::Int(val)),
                TypeIdentifier::Boolean => row.get(i).map(|val| PrismaValue::Boolean(val)),
                TypeIdentifier::Enum => row.get(i).map(|val: String| PrismaValue::Enum(EnumValue::string(val.clone(), val))),
                TypeIdentifier::Json => row.get(i).and_then(|val| {
                    let val: String = val;
                    serde_json::from_str(&val).map(|r| PrismaValue::Json(r)).map_err(|err| {
                        SqliteError::FromSqlConversionFailure(i as usize, SqliteType::Text, Box::new(err))
                    })
                }),
                TypeIdentifier::UUID => {
                    let result: Result<String, _> = row.get(i);

                    if let Ok(val) = result {
                        let uuid = Uuid::parse_str(val.as_ref())?;

                        Ok(PrismaValue::Uuid(uuid))
                    } else {
                        result.map(|s| PrismaValue::String(s))
                    }
                }
                TypeIdentifier::DateTime => match column.decl_type() {
                    Some("DATETIME") => row
                        .get(i)
                        .map(|naive: NaiveDateTime| PrismaValue::DateTime(DateTime::from_utc(naive, Utc))),
                    _ => row.get(i).map(|ts: i64| {
                        let nsecs = ((ts % 1000) * 1_000_000) as u32;
                        let secs = (ts / 1000) as i64;
                        let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
                        let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

                        PrismaValue::DateTime(datetime)
                    }),
                },
            };

            match result {
                Ok(pv) => Ok(pv),
                Err(rusqlite::Error::InvalidColumnType(_, _, rusqlite::types::Type::Null)) => Ok(PrismaValue::Null),
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

impl TryFrom<&Box<dyn Source>> for Sqlite {
    type Error = SqlError;

    /// Todo connection limit configuration
    fn try_from(source: &Box<dyn Source>) -> SqlResult<Sqlite> {
        // For the moment, we don't support file urls directly.
        let normalized = source.url().trim_start_matches("file:");
        let file_path = PathBuf::from(normalized);

        if file_path.exists() && !file_path.is_dir() {
            Sqlite::new(normalized.into(), 10, false)
        } else {
            Err(SqlError::DatabaseCreationError(
                "Sqlite data source must point to an existing file.",
            ))
        }
    }
}

impl Sqlite {
    /// Creates a new SQLite pool connected into local memory.
    pub fn new(file_path: String, connection_limit: u32, test_mode: bool) -> SqlResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        Ok(Sqlite {
            file_path,
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
                let name: String = row.get(1)?;

                Ok(name)
            })?
            .map(|res| res.unwrap())
            .collect();

        if !databases.contains(db_name) {
            // This is basically hacked until we have a full rust stack with a migration engine.
            // Currently, the scala tests use the JNA library to write to the database.
            conn.execute("ATTACH DATABASE ? AS ?", &[self.file_path.as_ref(), db_name])?;
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
