use crate::{query_builder::ManyRelatedRecordsWithRowNumber, FromSource, SqlCapabilities, Transactional};
use datamodel::Source;
use prisma_query::{
    ast::ParameterizedValue,
    connector::{Queryable, Transaction},
    pool::{sqlite::SqliteConnectionManager, PrismaConnectionManager},
};
use std::collections::HashSet;

type Pool = r2d2::Pool<PrismaConnectionManager<SqliteConnectionManager>>;

pub struct Sqlite {
    pool: Pool,
    test_mode: bool,
    file_path: String,
}

impl Sqlite {
    pub fn new(file_path: String, connection_limit: u32, test_mode: bool) -> crate::Result<Self> {
        let manager = PrismaConnectionManager::new(&file_path)?;
        let pool = r2d2::Pool::builder().max_size(connection_limit).build(manager)?;

        Ok(Self {
            pool,
            test_mode,
            file_path,
        })
    }
}

impl FromSource for Sqlite {
    fn from_source(source: &Box<dyn Source>) -> crate::Result<Self> {
        let manager = PrismaConnectionManager::new(source.url())?;
        let pool = r2d2::Pool::builder().max_size(10).build(manager)?;

        Ok(Sqlite {
            pool,
            test_mode: false,
            file_path: source.url().to_string(),
        })
    }
}

impl SqlCapabilities for Sqlite {
    type ManyRelatedRecordsBuilder = ManyRelatedRecordsWithRowNumber;
}

impl Transactional for Sqlite {
    fn with_transaction<F, T>(&self, db: &str, f: F) -> crate::Result<T>
    where
        F: FnOnce(&mut Transaction) -> crate::Result<T>,
    {
        let mut conn = self.pool.get()?;

        let databases: HashSet<String> = conn
            .query_raw("PRAGMA database_list", &[])?
            .into_iter()
            .map(|rr| {
                let db_name = rr.into_iter().nth(1).unwrap();

                db_name.into_string().unwrap()
            })
            .collect();

        if !databases.contains(db) {
            // This is basically hacked until we have a full rust stack with a migration engine.
            // Currently, the scala tests use the JNA library to write to the database.
            conn.execute_raw(
                "ATTACH DATABASE ? AS ?",
                &[
                    ParameterizedValue::from(self.file_path.as_ref()),
                    ParameterizedValue::from(db),
                ],
            )?;
        }

        conn.execute_raw("PRAGMA foreign_keys = ON", &[])?;

        let result = {
            let mut tx = conn.start_transaction()?;
            let result = f(&mut tx);

            if result.is_ok() {
                tx.commit()?;
            }

            result
        };

        if self.test_mode {
            conn.execute_raw("DETACH DATABASE ?", &[ParameterizedValue::from(db)])?;
        }

        result
    }
}
