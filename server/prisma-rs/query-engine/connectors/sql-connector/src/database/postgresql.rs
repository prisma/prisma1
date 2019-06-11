use crate::{
    error::SqlError, query_builder::RelatedNodesWithRowNumber, MutationBuilder, RawQuery, SqlId, SqlResult, SqlRow,
    ToSqlRow, Transaction, Transactional,
};
use chrono::{DateTime, NaiveDateTime, Utc};
use connector::{error::*, ConnectorResult};
use datamodel::configuration::Source;
use native_tls::TlsConnector;
use postgres::{
    types::{FromSql, ToSql, Type as PostgresType},
    Client, Config, Row as PostgresRow, Transaction as PostgresTransaction,
};
use prisma_common::config::{ConnectionLimit, ConnectionStringConfig, ExplicitConfig, PrismaDatabase};
use prisma_models::{GraphqlId, PrismaValue, ProjectRef, TypeIdentifier};
use prisma_query::{
    ast::Query,
    visitor::{self, Visitor},
};
use r2d2_postgres::PostgresConnectionManager;
use rust_decimal::Decimal;
use serde_json::{Map, Number, Value};
use std::{convert::TryFrom, str::FromStr};
use tokio_postgres::config::SslMode;
use tokio_postgres_native_tls::MakeTlsConnector;
use uuid::Uuid;

type Pool = r2d2::Pool<PostgresConnectionManager<MakeTlsConnector>>;

/// The World's Most Advanced Open Source Relational Database
pub struct PostgreSql {
    pool: Pool,
}

impl TryFrom<&Box<dyn Source>> for PostgreSql {
    type Error = SqlError;

    /// Todo connection limit configuration
    fn try_from(source: &Box<dyn Source>) -> SqlResult<PostgreSql> {
        let mut config = Config::from_str(source.url())?;
        config.ssl_mode(SslMode::Prefer);

        trace!("{:?}", &config);
        Ok(Self::new(config, 10)?)
    }
}

/// Legacy config converter
impl TryFrom<&PrismaDatabase> for PostgreSql {
    type Error = ConnectorError;

    fn try_from(db: &PrismaDatabase) -> ConnectorResult<Self> {
        match db {
            PrismaDatabase::ConnectionString(ref config) => Ok(PostgreSql::try_from(config)?),
            PrismaDatabase::Explicit(ref config) => Ok(PostgreSql::try_from(config)?),
            _ => Err(ConnectorError::DatabaseCreationError(
                "Could not understand the configuration format.",
            )),
        }
    }
}

/// Legacy config converter
impl TryFrom<&ExplicitConfig> for PostgreSql {
    type Error = SqlError;

    fn try_from(e: &ExplicitConfig) -> SqlResult<Self> {
        let db_name = e.database.as_ref().map(|x| x.as_str()).unwrap_or("postgres");
        let mut config = Config::new();

        config.host(&e.host);
        config.port(e.port);
        config.user(&e.user);
        config.ssl_mode(SslMode::Prefer);
        config.dbname(db_name);

        if let Some(ref pw) = e.password {
            config.password(pw);
        }

        Ok(Self::new(config, e.limit())?)
    }
}

/// Legacy config converter
impl TryFrom<&ConnectionStringConfig> for PostgreSql {
    type Error = SqlError;

    fn try_from(s: &ConnectionStringConfig) -> SqlResult<Self> {
        let db_name = s.database.as_ref().map(|x| x.as_str()).unwrap_or("postgres");
        let mut config = Config::from_str(s.uri.as_str())?;

        config.ssl_mode(SslMode::Prefer);
        config.dbname(db_name);

        Ok(Self::new(config, s.limit())?)
    }
}

impl Transactional for PostgreSql {
    type RelatedNodesBuilder = RelatedNodesWithRowNumber;

    fn with_transaction<F, T>(&self, _: &str, f: F) -> SqlResult<T>
    where
        F: FnOnce(&mut Transaction) -> SqlResult<T>,
    {
        self.with_client(|client| {
            let mut tx = client.transaction()?;
            let result = f(&mut tx);

            if result.is_ok() {
                tx.commit()?;
            }

            result
        })
    }
}

impl<'a> FromSql<'a> for SqlId {
    fn from_sql(ty: &PostgresType, raw: &'a [u8]) -> Result<SqlId, Box<dyn std::error::Error + Sync + Send>> {
        let res = match *ty {
            PostgresType::INT2 => SqlId::Int(i16::from_sql(ty, raw)? as usize),
            PostgresType::INT4 => SqlId::Int(i32::from_sql(ty, raw)? as usize),
            PostgresType::INT8 => SqlId::Int(i64::from_sql(ty, raw)? as usize),
            PostgresType::UUID => SqlId::UUID(Uuid::from_sql(ty, raw)?),
            _ => SqlId::String(String::from_sql(ty, raw)?),
        };

        Ok(res)
    }

    fn accepts(ty: &PostgresType) -> bool {
        <&str as FromSql>::accepts(ty)
            || <Uuid as FromSql>::accepts(ty)
            || <i16 as FromSql>::accepts(ty)
            || <i32 as FromSql>::accepts(ty)
            || <i64 as FromSql>::accepts(ty)
    }
}

impl<'a> Transaction for PostgresTransaction<'a> {
    fn write(&mut self, q: Query) -> SqlResult<Option<GraphqlId>> {
        let id = match q {
            insert @ Query::Insert(_) => {
                let (sql, params) = visitor::Postgres::build(insert);
                debug!("{}\n{:?}", sql, params);

                let params: Vec<&ToSql> = params.iter().map(|pv| pv as &ToSql).collect();
                let stmt = self.prepare(&sql)?;
                let rows = self.query(&stmt, params.as_slice())?;

                rows.into_iter().rev().next().map(|row| {
                    let id: SqlId = row.get(0);
                    GraphqlId::from(id)
                })
            }
            query => {
                let (sql, params) = visitor::Postgres::build(query);
                debug!("{}\n{:?}", sql, params);

                let params: Vec<&ToSql> = params.iter().map(|pv| pv as &ToSql).collect();

                let stmt = self.prepare(&sql)?;
                self.execute(&stmt, params.as_slice())?;

                None
            }
        };

        Ok(id)
    }

    fn filter(&mut self, q: Query, idents: &[TypeIdentifier]) -> SqlResult<Vec<SqlRow>> {
        let (sql, params) = visitor::Postgres::build(q);
        debug!("{}\n{:?}", sql, params);

        let params: Vec<&ToSql> = params.iter().map(|pv| pv as &ToSql).collect();

        let stmt = self.prepare(&sql)?;
        let rows = self.query(&stmt, params.as_slice())?;
        let mut result = Vec::new();

        for row in rows {
            result.push(row.to_sql_row(idents)?);
        }

        Ok(result)
    }

    fn truncate(&mut self, project: ProjectRef) -> SqlResult<()> {
        self.write(Query::from("SET CONSTRAINTS ALL DEFERRED"))?;

        for delete in MutationBuilder::truncate_tables(project) {
            self.delete(delete)?;
        }

        Ok(())
    }

    fn raw(&mut self, q: RawQuery) -> SqlResult<Value> {
        let stmt = self.prepare(&q.0)?;

        if q.is_select() {
            let rows = self.query(&stmt, &[])?;
            let mut result = Vec::new();

            for row in rows {
                let mut object = Map::new();
                for (i, column) in row.columns().into_iter().enumerate() {
                    let value = match *column.type_() {
                        PostgresType::BOOL => match row.try_get(i)? {
                            Some(val) => Value::Bool(val),
                            None => Value::Null,
                        },
                        PostgresType::INT2 => match row.try_get(i)? {
                            Some(val) => {
                                let val: i16 = val;
                                Value::Number(Number::from(val))
                            }
                            None => Value::Null,
                        },
                        PostgresType::INT4 => match row.try_get(i)? {
                            Some(val) => {
                                let val: i32 = val;
                                Value::Number(Number::from(val))
                            }
                            None => Value::Null,
                        },
                        PostgresType::INT8 => match row.try_get(i)? {
                            Some(val) => {
                                let val: i64 = val;
                                Value::Number(Number::from(val))
                            }
                            None => Value::Null,
                        },
                        PostgresType::NUMERIC => match row.try_get(i)? {
                            Some(val) => {
                                let val: Decimal = val;
                                let val: f64 = val.to_string().parse().unwrap();
                                Value::Number(Number::from_f64(val).unwrap())
                            }
                            None => Value::Null,
                        },
                        PostgresType::FLOAT4 => match row.try_get(i)? {
                            Some(val) => {
                                let val: f32 = val;
                                Value::Number(Number::from_f64(val as f64).unwrap())
                            }
                            None => Value::Null,
                        },
                        PostgresType::FLOAT8 => match row.try_get(i)? {
                            Some(val) => {
                                let val: f64 = val;
                                Value::Number(Number::from_f64(val).unwrap())
                            }
                            None => Value::Null,
                        },
                        PostgresType::TIMESTAMP => match row.try_get(i)? {
                            Some(val) => {
                                let ts: NaiveDateTime = val;
                                let dt = DateTime::<Utc>::from_utc(ts, Utc);
                                Value::String(dt.to_rfc3339())
                            }
                            None => Value::Null,
                        },
                        PostgresType::UUID => match row.try_get(i)? {
                            Some(val) => {
                                let val: Uuid = val;
                                Value::String(val.to_hyphenated().to_string())
                            }
                            None => Value::Null,
                        },
                        PostgresType::INT2_ARRAY => match row.try_get(i)? {
                            Some(val) => {
                                let val: Vec<i16> = val;
                                Value::Array(val.into_iter().map(Value::from).collect())
                            }
                            None => Value::Null,
                        },
                        PostgresType::INT4_ARRAY => match row.try_get(i)? {
                            Some(val) => {
                                let val: Vec<i32> = val;
                                Value::Array(val.into_iter().map(Value::from).collect())
                            }
                            None => Value::Null,
                        },
                        PostgresType::INT8_ARRAY => match row.try_get(i)? {
                            Some(val) => {
                                let val: Vec<i64> = val;
                                Value::Array(val.into_iter().map(Value::from).collect())
                            }
                            None => Value::Null,
                        },
                        PostgresType::FLOAT4_ARRAY => match row.try_get(i)? {
                            Some(val) => {
                                let val: Vec<f32> = val;
                                Value::Array(
                                    val.into_iter()
                                        .map(|f| Number::from_f64(f as f64).unwrap())
                                        .map(Value::Number)
                                        .collect(),
                                )
                            }
                            None => Value::Null,
                        },
                        PostgresType::FLOAT8_ARRAY => match row.try_get(i)? {
                            Some(val) => {
                                let val: Vec<f64> = val;
                                Value::Array(
                                    val.into_iter()
                                        .map(|f| Value::Number(Number::from_f64(f).unwrap()))
                                        .collect(),
                                )
                            }
                            None => Value::Null,
                        },
                        PostgresType::BOOL_ARRAY => match row.try_get(i)? {
                            Some(val) => {
                                let val: Vec<bool> = val;
                                Value::Array(val.into_iter().map(Value::from).collect())
                            }
                            None => Value::Null,
                        },
                        PostgresType::TIMESTAMP_ARRAY => match row.try_get(i)? {
                            Some(val) => {
                                let val: Vec<NaiveDateTime> = val;

                                let val: Vec<Value> = val
                                    .into_iter()
                                    .map(|ts| DateTime::<Utc>::from_utc(ts, Utc))
                                    .map(|dt| dt.to_rfc3339())
                                    .map(Value::from)
                                    .collect();

                                Value::Array(val)
                            }
                            None => Value::Null,
                        },
                        PostgresType::NUMERIC_ARRAY => match row.try_get(i)? {
                            Some(val) => {
                                let val: Vec<Decimal> = val;

                                let val: Vec<Value> = val
                                    .into_iter()
                                    .map(|d| d.to_string())
                                    .map(|s| s.parse::<f64>().unwrap())
                                    .map(|f| Number::from_f64(f).unwrap())
                                    .map(Value::Number)
                                    .collect();

                                Value::Array(val)
                            }
                            None => Value::Null,
                        },
                        PostgresType::TEXT_ARRAY | PostgresType::NAME_ARRAY | PostgresType::VARCHAR_ARRAY => {
                            match row.try_get(i)? {
                                Some(val) => {
                                    let val: Vec<&str> = val;
                                    Value::Array(val.into_iter().map(Value::from).collect())
                                }
                                None => Value::Null,
                            }
                        }
                        _ => match row.try_get(i)? {
                            Some(val) => Value::String(val),
                            None => Value::Null,
                        },
                    };

                    object.insert(String::from(column.name()), value);
                }

                result.push(Value::Object(object));
            }

            Ok(Value::Array(result))
        } else {
            let changes = self.execute(&stmt, &[])?;

            Ok(Value::Number(Number::from(changes)))
        }
    }
}

impl ToSqlRow for PostgresRow {
    fn to_sql_row<'b, T>(&'b self, idents: T) -> SqlResult<SqlRow>
    where
        T: IntoIterator<Item = &'b TypeIdentifier>,
    {
        fn convert(row: &PostgresRow, i: usize, typid: &TypeIdentifier) -> SqlResult<PrismaValue> {
            let result = match typid {
                TypeIdentifier::String => match row.try_get(i)? {
                    Some(val) => PrismaValue::String(val),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::GraphQLID | TypeIdentifier::Relation => match row.try_get(i)? {
                    Some(val) => {
                        let id: SqlId = val;
                        PrismaValue::GraphqlId(GraphqlId::from(id))
                    }
                    None => PrismaValue::Null,
                },
                TypeIdentifier::Float => match *row.columns()[i].type_() {
                    PostgresType::NUMERIC => match row.try_get(i)? {
                        Some(val) => {
                            let dec: Decimal = val;
                            let dec_s = dec.to_string();
                            PrismaValue::Float(dec_s.parse().unwrap())
                        }
                        None => PrismaValue::Null,
                    },
                    _ => match row.try_get(i)? {
                        Some(val) => PrismaValue::Float(val),
                        None => PrismaValue::Null,
                    },
                },
                TypeIdentifier::Int => match *row.columns()[i].type_() {
                    PostgresType::INT2 => match row.try_get(i)? {
                        Some(val) => {
                            let val: i16 = val;
                            PrismaValue::Int(val as i64)
                        }
                        None => PrismaValue::Null,
                    },
                    PostgresType::INT4 => match row.try_get(i)? {
                        Some(val) => {
                            let val: i32 = val;
                            PrismaValue::Int(val as i64)
                        }
                        None => PrismaValue::Null,
                    },
                    _ => PrismaValue::Int(row.try_get(i)?),
                },
                TypeIdentifier::Boolean => match row.try_get(i)? {
                    Some(val) => PrismaValue::Boolean(val),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::Enum => match row.try_get(i)? {
                    Some(val) => PrismaValue::Enum(val),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::Json => match row.try_get(i)? {
                    Some(val) => {
                        let j_str: &str = val;
                        PrismaValue::Json(serde_json::from_str(j_str)?)
                    }
                    None => PrismaValue::Null,
                },
                TypeIdentifier::UUID => match row.try_get(i)? {
                    Some(val) => PrismaValue::Uuid(val),
                    None => PrismaValue::Null,
                },
                TypeIdentifier::DateTime => match row.try_get(i)? {
                    Some(val) => {
                        let ts: NaiveDateTime = val;
                        PrismaValue::DateTime(DateTime::<Utc>::from_utc(ts, Utc))
                    }
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

impl PostgreSql {
    fn new(config: Config, connections: u32) -> SqlResult<PostgreSql> {
        let mut tls_builder = TlsConnector::builder();
        tls_builder.danger_accept_invalid_certs(true); // For Heroku

        let tls = MakeTlsConnector::new(tls_builder.build()?);
        let manager = PostgresConnectionManager::new(config, tls);
        let pool = r2d2::Pool::builder().max_size(connections).build(manager)?;

        Ok(PostgreSql { pool })
    }

    fn with_client<F, T>(&self, f: F) -> SqlResult<T>
    where
        F: FnOnce(&mut Client) -> SqlResult<T>,
    {
        let mut client = self.pool.get()?;
        let result = f(&mut client);
        result
    }
}
