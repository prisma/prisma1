use crate::{query_builder::ManyRelatedRecordsWithRowNumber, FromSource, SqlCapabilities, Transaction, Transactional};
use datamodel::Source;
use prisma_query::{
    connector::{PostgresParams, Queryable},
    pool::{postgres::PostgresManager, PrismaConnectionManager},
};
use std::convert::TryFrom;

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

impl Transactional for PostgreSql {
    fn with_transaction<F, T>(&self, _: &str, f: F) -> crate::Result<T>
    where
        F: FnOnce(&mut dyn Transaction) -> crate::Result<T>,
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
