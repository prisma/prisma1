use crate::{
    error::SqlError, query_builder::ManyRelatedRecordsWithUnionAll, MutationBuilder, RawQuery, SqlResult, SqlRow,
    ToSqlRow, Transaction, Transactional,
};
use chrono::{DateTime, Duration, NaiveDate, NaiveDateTime, Utc};
use connector::{error::*, ConnectorResult};
use datamodel::configuration::Source;
use mysql_client as my;
use prisma_common::config::{ConnectionLimit, ConnectionStringConfig, ExplicitConfig, PrismaDatabase};
use prisma_models::{GraphqlId, InternalDataModelRef, PrismaValue, TypeIdentifier};
use prisma_query::{
    ast::*,
    visitor::{self, Visitor},
};
use r2d2_mysql::pool::MysqlConnectionManager;
use serde_json::{Map, Number, Value};
use std::convert::TryFrom;
use url::Url;
use uuid::Uuid;

type Pool = r2d2::Pool<MysqlConnectionManager>;

/// The World's Most Advanced Open Source Relational Database
pub struct Mysql {
    pool: Pool,
}

impl TryFrom<&Box<dyn Source>> for Mysql {
    type Error = SqlError;

    /// Todo connection limit configuration
    fn try_from(source: &Box<dyn Source>) -> SqlResult<Mysql> {
        let mut builder = my::OptsBuilder::new();
        let url = Url::parse(source.url())?;
        let db_name = match url.path_segments() {
            Some(mut segments) => segments.next().unwrap_or("mysql"),
            None => "mysql",
        };

        builder.ip_or_hostname(url.host_str());
        builder.tcp_port(url.port().unwrap_or(3306));
        builder.user(Some(url.username()));
        builder.pass(url.password());
        builder.db_name(Some(db_name));
        builder.verify_peer(false);
        builder.stmt_cache_size(Some(1000));

        let manager = MysqlConnectionManager::new(builder);
        let pool = r2d2::Pool::builder().max_size(10).build(manager)?;

        Ok(Self { pool })
    }
}

impl TryFrom<&PrismaDatabase> for Mysql {
    type Error = ConnectorError;

    fn try_from(db: &PrismaDatabase) -> ConnectorResult<Self> {
        match db {
            PrismaDatabase::ConnectionString(ref config) => Ok(Mysql::try_from(config)?),
            PrismaDatabase::Explicit(ref config) => Ok(Mysql::try_from(config)?),
            _ => Err(ConnectorError::DatabaseCreationError(
                "Could not understand the configuration format.",
            )),
        }
    }
}

impl TryFrom<&ExplicitConfig> for Mysql {
    type Error = SqlError;

    fn try_from(e: &ExplicitConfig) -> SqlResult<Self> {
        let db_name = e.database.as_ref().map(|x| x.as_str()).unwrap_or("mysql");
        let mut builder = my::OptsBuilder::new();

        builder.ip_or_hostname(Some(e.host.as_ref()));
        builder.tcp_port(e.port);
        builder.user(Some(e.user.as_ref()));
        builder.db_name(Some(db_name));
        builder.pass(e.password.as_ref().map(|p| p.as_str()));
        builder.verify_peer(false);
        builder.stmt_cache_size(Some(1000));

        let manager = MysqlConnectionManager::new(builder);
        let pool = r2d2::Pool::builder().max_size(e.limit()).build(manager)?;

        Ok(Self { pool })
    }
}

impl TryFrom<&ConnectionStringConfig> for Mysql {
    type Error = SqlError;

    fn try_from(s: &ConnectionStringConfig) -> SqlResult<Self> {
        let db_name = s.database.as_ref().map(|x| x.as_str()).unwrap_or("mysql");
        let mut builder = my::OptsBuilder::new();

        builder.ip_or_hostname(s.uri.host_str());
        builder.tcp_port(s.uri.port().unwrap_or(3306));
        builder.user(Some(s.uri.username()));
        builder.db_name(Some(db_name));
        builder.pass(s.uri.password());
        builder.verify_peer(false);
        builder.stmt_cache_size(Some(1000));

        let manager = MysqlConnectionManager::new(builder);
        let pool = r2d2::Pool::builder().max_size(s.limit()).build(manager)?;

        Ok(Self { pool })
    }
}

impl Transactional for Mysql {
    type ManyRelatedRecordsBuilder = ManyRelatedRecordsWithUnionAll;

    fn with_transaction<F, T>(&self, _: &str, f: F) -> SqlResult<T>
    where
        F: FnOnce(&mut Transaction) -> SqlResult<T>,
    {
        self.with_conn(|conn| {
            let mut tx = conn.start_transaction(true, None, None)?;
            let result = f(&mut tx);

            if result.is_ok() {
                tx.commit()?;
            }

            result
        })
    }
}

impl<'a> Transaction for my::Transaction<'a> {
    fn write(&mut self, q: Query) -> SqlResult<Option<GraphqlId>> {
        let (sql, params) = visitor::Mysql::build(q);
        debug!("{}\n{:?}", sql, params);

        let mut stmt = self.prepare(&sql)?;
        let result = stmt.execute(params)?;

        Ok(Some(GraphqlId::from(result.last_insert_id())))
    }

    fn filter(&mut self, q: Query, idents: &[TypeIdentifier]) -> SqlResult<Vec<SqlRow>> {
        let (sql, params) = visitor::Mysql::build(q);
        debug!("{}\n{:?}", sql, params);

        let mut stmt = self.prepare(&sql)?;
        let rows = stmt.execute(params)?;
        let mut result = Vec::new();

        for row in rows {
            result.push(row?.to_sql_row(idents)?);
        }

        Ok(result)
    }

    fn truncate(&mut self, internal_data_model: InternalDataModelRef) -> SqlResult<()> {
        self.write(Query::from("SET FOREIGN_KEY_CHECKS=0"))?;

        for delete in MutationBuilder::truncate_tables(internal_data_model) {
            if let Err(e) = self.delete(delete) {
                self.write(Query::from("SET FOREIGN_KEY_CHECKS=1"))?;
                return Err(e);
            }
        }

        self.write(Query::from("SET FOREIGN_KEY_CHECKS=1"))?;

        Ok(())
    }

    fn raw(&mut self, q: RawQuery) -> SqlResult<Value> {
        let mut stmt = self.prepare(&q.0)?;

        if q.is_select() {
            let rows = stmt.execute(())?;
            let mut result = Vec::new();

            for row in rows {
                let mut object = Map::new();
                let row = row?;
                let columns = row.columns();

                for (idx, raw_value) in row.unwrap().iter().enumerate() {
                    let js_value = match raw_value {
                        my::Value::NULL => Value::Null,
                        my::Value::Bytes(b) => Value::String(String::from_utf8(b.to_vec())?),
                        my::Value::Int(i) => Value::Number(Number::from(*i)),
                        my::Value::UInt(i) => Value::Number(Number::from(*i)),
                        my::Value::Float(f) => Value::Number(Number::from_f64(*f).unwrap()),
                        my::Value::Date(year, month, day, hour, min, sec, _) => {
                            let naive = NaiveDate::from_ymd(*year as i32, *month as u32, *day as u32).and_hms(
                                *hour as u32,
                                *min as u32,
                                *sec as u32,
                            );

                            let dt: DateTime<Utc> = DateTime::from_utc(naive, Utc);
                            Value::String(dt.to_rfc3339())
                        }
                        my::Value::Time(is_neg, days, hours, minutes, seconds, micros) => {
                            let days = Duration::days(*days as i64);
                            let hours = Duration::hours(*hours as i64);
                            let minutes = Duration::minutes(*minutes as i64);
                            let seconds = Duration::seconds(*seconds as i64);
                            let micros = Duration::microseconds(*micros as i64);

                            let time = days
                                .checked_add(&hours)
                                .and_then(|t| t.checked_add(&minutes))
                                .and_then(|t| t.checked_add(&seconds))
                                .and_then(|t| t.checked_add(&micros))
                                .unwrap();

                            let duration = time.to_std().unwrap();
                            let f_time = duration.as_secs() as f64 + duration.subsec_micros() as f64 * 1e-6;

                            Value::Number(Number::from_f64(if *is_neg { -f_time } else { f_time }).unwrap())
                        }
                    };

                    let column_name: String = columns[idx].name_str().into_owned();

                    object.insert(column_name, js_value);
                }

                result.push(Value::Object(object));
            }

            Ok(Value::Array(result))
        } else {
            let result = stmt.execute(())?;

            Ok(Value::Number(Number::from(result.affected_rows())))
        }
    }
}

impl ToSqlRow for my::Row {
    fn to_sql_row<'b, T>(&'b self, idents: T) -> SqlResult<SqlRow>
    where
        T: IntoIterator<Item = &'b TypeIdentifier>,
    {
        fn convert(row: &my::Row, i: usize, typid: &TypeIdentifier) -> SqlResult<PrismaValue> {
            let result = match typid {
                TypeIdentifier::String => match row.get_opt(i) {
                    Some(val) => val.map(|val| PrismaValue::String(val)).unwrap_or(PrismaValue::Null),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::GraphQLID | TypeIdentifier::Relation => match row.as_ref(i) {
                    Some(val) => match val {
                        my::Value::Int(i) => PrismaValue::GraphqlId(GraphqlId::from(*i)),
                        my::Value::UInt(i) => PrismaValue::GraphqlId(GraphqlId::from(*i)),
                        my::Value::Bytes(_) => match row.get_opt(i) {
                            Some(val) => {
                                let val: Vec<u8> = val?;
                                PrismaValue::GraphqlId(GraphqlId::try_from(val)?)
                            }
                            _ => PrismaValue::Null,
                        },
                        my::Value::NULL => PrismaValue::Null,
                        _ => unreachable!(),
                    },
                    None => PrismaValue::Null,
                },
                TypeIdentifier::Float => match row.get_opt(i) {
                    Some(val) => val.map(|val| PrismaValue::Float(val)).unwrap_or(PrismaValue::Null),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::Int => match row.get_opt(i) {
                    Some(val) => val.map(|val| PrismaValue::Int(val)).unwrap_or(PrismaValue::Null),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::Boolean => match row.get_opt(i) {
                    Some(val) => val.map(|val| PrismaValue::Boolean(val)).unwrap_or(PrismaValue::Null),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::Enum => match row.get_opt(i) {
                    Some(val) => val.map(|val| PrismaValue::Enum(val)).unwrap_or(PrismaValue::Null),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::Json => match row.get_opt(i) {
                    Some(val) => val
                        .map(|val| {
                            let val: Vec<u8> = val;
                            PrismaValue::Json(serde_json::from_slice(val.as_slice()).unwrap())
                        })
                        .unwrap_or(PrismaValue::Null),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::UUID => match row.get_opt(i) {
                    Some(val) => val
                        .map(|val| {
                            let val: Vec<u8> = val;
                            let uuid = Uuid::from_slice(val.as_slice()).unwrap();
                            PrismaValue::Uuid(uuid)
                        })
                        .unwrap_or(PrismaValue::Null),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::DateTime => match row.get_opt(i) {
                    Some(val) => val
                        .map(|val| {
                            let ts: NaiveDateTime = val;
                            PrismaValue::DateTime(DateTime::<Utc>::from_utc(ts, Utc))
                        })
                        .unwrap_or(PrismaValue::Null),
                    None => PrismaValue::Null,
                },
            };

            Ok(result)
        }

        let mut row = SqlRow::default();

        for (i, typid) in idents.into_iter().enumerate() {
            row.values.push(convert(self, i, typid)?);
        }

        Ok(row)
    }
}

impl Mysql {
    fn with_conn<F, T>(&self, f: F) -> SqlResult<T>
    where
        F: FnOnce(&mut r2d2::PooledConnection<MysqlConnectionManager>) -> SqlResult<T>,
    {
        let mut conn = self.pool.get()?;
        let result = f(&mut conn);
        result
    }
}
