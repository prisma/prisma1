use crate::{
    error::SqlError, query_builder::ManyRelatedRecordsWithRowNumber, FromSource, LegacyDatabase, SqlCapabilities,
    Transaction, Transactional,
};
use datamodel::Source;
use postgres::Config;
use prisma_common::config::*;
use prisma_query::{
    connector::Queryable,
    pool::{postgres::PostgresManager, PrismaConnectionManager},
};
use std::{convert::TryFrom, str::FromStr};
use tokio_postgres::config::SslMode;

type Pool = r2d2::Pool<PrismaConnectionManager<PostgresManager>>;

pub struct PostgreSql {
    pool: Pool,
}

impl FromSource for PostgreSql {
    fn from_source(source: &Box<dyn Source>) -> crate::Result<Self> {
        let mut url: url::Url = url::Url::parse(source.url())?;

        // Supported official connection url parameters (empty = strip all)
        let official = vec![];

        let (supported, unsupported): (Vec<(String, String)>, Vec<(String, String)>) = url
            .query_pairs()
            .into_iter()
            .map(|(k, v)| (String::from(k), String::from(v)))
            .collect::<Vec<(String, String)>>()
            .into_iter()
            .partition(|(k, _)| official.contains(&k.as_str()));

        // Reset params and append supported ones, then create a valid config & set keys based on custom params.
        url.query_pairs_mut().clear();

        supported.into_iter().for_each(|(k, v)| {
            url.query_pairs_mut().append_pair(&k, &v);
        });

        let mut config = Config::from_str(&url.to_string())?;
        let mut connection_limit: u32 = 1;

        for (k, v) in unsupported.into_iter() {
            match k.as_ref() {
                "sslmode" => {
                    match v.as_ref() {
                        "disable" => config.ssl_mode(SslMode::Disable),
                        "prefer" => config.ssl_mode(SslMode::Prefer),
                        "require" => config.ssl_mode(SslMode::Require),
                        _ => {
                            debug!("Unsupported ssl mode {}, defaulting to 'prefer'", v);
                            config.ssl_mode(SslMode::Prefer)
                        }
                    };
                }
                "connection_limit" => {
                    let as_int: u32 = v.parse().map_err(|_| SqlError::InvalidConnectionArguments)?;
                    connection_limit = as_int;
                }
                _ => trace!("Discarding connection string param: {}", k),
            };
        }

        trace!("{:?}", &config);

        let manager = PrismaConnectionManager::try_from(config)?;
        let pool = r2d2::Pool::builder().max_size(connection_limit).build(manager)?;

        Ok(PostgreSql { pool })
    }
}

impl SqlCapabilities for PostgreSql {
    type ManyRelatedRecordsBuilder = ManyRelatedRecordsWithRowNumber;
}

impl LegacyDatabase for PostgreSql {
    fn from_prisma_database(db: &PrismaDatabase) -> crate::Result<Self> {
        match db {
            PrismaDatabase::Explicit(e) => {
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

                let manager = PrismaConnectionManager::try_from(config)?;
                let pool = r2d2::Pool::builder().max_size(e.limit()).build(manager)?;

                Ok(PostgreSql { pool })
            }
            PrismaDatabase::ConnectionString(s) => {
                let db_name = s.database.as_ref().map(|x| x.as_str()).unwrap_or("postgres");
                let mut config = Config::from_str(s.uri.as_str())?;

                config.ssl_mode(SslMode::Prefer);
                config.dbname(db_name);

                let manager = PrismaConnectionManager::try_from(config)?;
                let pool = r2d2::Pool::builder().max_size(s.limit()).build(manager)?;

                Ok(PostgreSql { pool })
            }
            PrismaDatabase::File(_) => panic!("MySQL will not work with file based configuration"),
        }
    }
}

impl Transactional for PostgreSql {
    fn with_transaction<F, T>(&self, _: &str, f: F) -> crate::Result<T>
    where
        F: FnOnce(&mut Transaction) -> crate::Result<T>,
    {
        let mut conn = self.pool.get()?;
        let mut tx = conn.start_transaction()?;
        let result = f(&mut tx);

        if result.is_ok() {
            tx.commit()?;
        }

        result
    }
}
