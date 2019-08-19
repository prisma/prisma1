use crate::{
    query_builder::ManyRelatedRecordsWithRowNumber, FromSource, LegacyDatabase, SqlCapabilities, Transaction,
    Transactional,
};
use datamodel::Source;
use postgres::Config;
use prisma_common::config::*;
use prisma_query::{
    connector::{PostgresParams, Queryable},
    pool::{postgres::PostgresManager, PrismaConnectionManager},
};
use std::{convert::TryFrom, str::FromStr};
use tokio_postgres::config::SslMode;

type Pool = r2d2::Pool<PrismaConnectionManager<PostgresManager>>;

pub struct PostgreSql {
    pool: Pool,
}

impl FromSource for PostgreSql {
    fn from_source(source: &dyn Source) -> crate::Result<Self> {
        let url = url::Url::parse(&source.url().value)?;
        let params = PostgresParams::try_from(url)?;
        let pool = r2d2::Pool::try_from(params).unwrap();

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

                let manager = PrismaConnectionManager::postgres(config, None)?;
                let pool = r2d2::Pool::builder().max_size(e.limit()).build(manager)?;

                Ok(PostgreSql { pool })
            }
            PrismaDatabase::ConnectionString(s) => {
                let db_name = s.database.as_ref().map(|x| x.as_str()).unwrap_or("postgres");
                let mut config = Config::from_str(s.uri.as_str())?;

                config.ssl_mode(SslMode::Prefer);
                config.dbname(db_name);

                let manager = PrismaConnectionManager::postgres(config, None)?;
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
