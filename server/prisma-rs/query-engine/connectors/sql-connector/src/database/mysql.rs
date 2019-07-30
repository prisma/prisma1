use crate::{
    query_builder::ManyRelatedRecordsWithUnionAll, FromSource, LegacyDatabase, SqlCapabilities, Transaction,
    Transactional,
};
use datamodel::Source;
use mysql_client as my;
use prisma_common::config::*;
use prisma_query::{
    connector::{Queryable, MysqlParams},
    pool::{mysql::MysqlConnectionManager, PrismaConnectionManager},
};
use std::convert::TryFrom;
use url::Url;

type Pool = r2d2::Pool<PrismaConnectionManager<MysqlConnectionManager>>;

pub struct Mysql {
    pool: Pool,
}

impl FromSource for Mysql {
    fn from_source(source: &Box<dyn Source>) -> crate::Result<Self> {
        let url = Url::parse(source.url())?;
        let params = MysqlParams::try_from(url)?;
        let pool = r2d2::Pool::try_from(params).unwrap();

        Ok(Mysql { pool })
    }
}

impl SqlCapabilities for Mysql {
    type ManyRelatedRecordsBuilder = ManyRelatedRecordsWithUnionAll;
}

impl LegacyDatabase for Mysql {
    fn from_prisma_database(db: &PrismaDatabase) -> crate::Result<Self> {
        match db {
            PrismaDatabase::Explicit(e) => {
                let db_name = e.database.as_ref().map(|x| x.as_str()).unwrap_or("mysql");
                let mut builder = my::OptsBuilder::new();

                builder.ip_or_hostname(Some(e.host.as_str()));
                builder.tcp_port(e.port);
                builder.user(Some(e.user.as_str()));
                builder.db_name(Some(db_name));
                builder.pass(e.password.as_ref().map(|p| p.as_str()));
                builder.verify_peer(false);
                builder.stmt_cache_size(Some(1000));

                let manager = PrismaConnectionManager::mysql(builder);
                let pool = r2d2::Pool::builder().max_size(e.limit()).build(manager)?;

                Ok(Mysql { pool })
            }
            PrismaDatabase::ConnectionString(s) => {
                let db_name = s.database.as_ref().map(|x| x.as_str()).unwrap_or("mysql");
                let mut builder = my::OptsBuilder::new();

                builder.ip_or_hostname(s.uri.host_str());
                builder.tcp_port(s.uri.port().unwrap_or(3306));
                builder.user(Some(s.uri.username()));
                builder.db_name(Some(db_name));
                builder.pass(s.uri.password());
                builder.verify_peer(false);
                builder.stmt_cache_size(Some(1000));

                let manager = PrismaConnectionManager::mysql(builder);
                let pool = r2d2::Pool::builder().max_size(s.limit()).build(manager)?;

                Ok(Mysql { pool })
            }
            PrismaDatabase::File(_) => panic!("MySQL will not work with file based configuration"),
        }
    }
}

impl Transactional for Mysql {
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
