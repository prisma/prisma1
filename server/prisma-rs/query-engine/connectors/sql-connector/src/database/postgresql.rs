use crate::{error::SqlError, MutationBuilder, SqlId, SqlResult, SqlRow, ToSqlRow, Transaction, Transactional};
use chrono::{DateTime, NaiveDateTime, Utc};
use connector::{error::*, ConnectorResult};
use native_tls::TlsConnector;
use postgres::{
    types::{FromSql, ToSql, Type as PostgresType},
    Client, Config, Row as PostgresRow, Transaction as PostgresTransaction,
};
use prisma_common::config::{ConnectionLimit, ConnectionStringConfig, ExplicitConfig, PrismaDatabase};
use prisma_models::{GraphqlId, PrismaValue, ProjectRef, TypeIdentifier};
use prisma_query::{
    ast::{Query, Select},
    visitor::{self, Visitor},
};
use r2d2_postgres::PostgresConnectionManager;
use rust_decimal::Decimal;
use std::{convert::TryFrom, str::FromStr};
use tokio_postgres::config::SslMode;
use tokio_postgres_native_tls::MakeTlsConnector;
use uuid::Uuid;

type Pool = r2d2::Pool<PostgresConnectionManager<MakeTlsConnector>>;

/// The World's Most Advanced Open Source Relational Database
pub struct PostgreSql {
    pool: Pool,
}

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

impl TryFrom<&ExplicitConfig> for PostgreSql {
    type Error = SqlError;

    fn try_from(e: &ExplicitConfig) -> SqlResult<Self> {
        let mut config = Config::new();
        config.host(&e.host);
        config.port(e.port);
        config.user(&e.user);
        config.ssl_mode(SslMode::Prefer);
        config.dbname("prisma");

        if let Some(ref pw) = e.password {
            config.password(pw);
        }

        Ok(Self::new(config, e.limit())?)
    }
}

impl TryFrom<&ConnectionStringConfig> for PostgreSql {
    type Error = SqlError;

    fn try_from(s: &ConnectionStringConfig) -> SqlResult<Self> {
        let mut config = Config::from_str(s.uri.as_str())?;
        config.ssl_mode(SslMode::Prefer);
        config.dbname("prisma");

        Ok(Self::new(config, s.limit())?)
    }
}

impl Transactional for PostgreSql {
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
                let (sql, params) = dbg!(visitor::Postgres::build(insert));

                let params: Vec<&ToSql> = params.iter().map(|pv| pv as &ToSql).collect();
                let stmt = self.prepare(&sql)?;
                let rows = self.query(&stmt, params.as_slice())?;

                rows.into_iter().rev().next().map(|row| {
                    let id: SqlId = row.get(0);
                    GraphqlId::from(id)
                })
            }
            query => {
                let (sql, params) = dbg!(visitor::Postgres::build(query));
                let params: Vec<&ToSql> = params.iter().map(|pv| pv as &ToSql).collect();

                let stmt = self.prepare(&sql)?;
                self.execute(&stmt, params.as_slice())?;

                None
            }
        };

        Ok(id)
    }

    fn filter(&mut self, q: Select, idents: &[TypeIdentifier]) -> SqlResult<Vec<SqlRow>> {
        let (sql, params) = dbg!(visitor::Postgres::build(q));
        let params: Vec<&ToSql> = params.iter().map(|pv| pv as &ToSql).collect();

        let stmt = self.prepare(&sql)?;
        let rows = self.query(&stmt, params.as_slice())?;
        let mut result = Vec::new();

        for row in rows {
            result.push(row.to_prisma_row(idents)?);
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
}

impl ToSqlRow for PostgresRow {
    fn to_prisma_row<'b, T>(&'b self, idents: T) -> SqlResult<SqlRow>
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
