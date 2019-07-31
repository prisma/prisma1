use prisma_query::{
    ast::*,
    connector::{self, Queryable, ResultSet, SqliteParams, PostgresParams, MysqlParams},
};
use std::{convert::TryFrom, sync::Mutex};

pub trait MigrationDatabase {
    fn execute(&self, db: &str, q: Query) -> prisma_query::Result<Option<Id>>;
    fn query(&self, db: &str, q: Query) -> prisma_query::Result<ResultSet>;
    fn query_raw(&self, db: &str, sql: &str, params: &[ParameterizedValue]) -> prisma_query::Result<ResultSet>;
    fn execute_raw(&self, db: &str, sql: &str, params: &[ParameterizedValue]) -> prisma_query::Result<u64>;
}

pub struct Sqlite {
    inner: Mutex<Box<dyn Queryable>>,
    pub(crate) file_path: String,
}

impl Sqlite {
    pub fn new(url: &str) -> prisma_query::Result<Self> {
        let params = SqliteParams::try_from(dbg!(url))?;
        let file_path = params.file_path.clone();
        let conn = connector::Sqlite::new(params.file_path)?;

        Ok(Self {
            inner: Mutex::new(Box::new(conn)),
            file_path: file_path.to_str().unwrap().to_string(),
        })
    }

    fn attach_database(&self, db: &str) {
        self.inner
            .lock()
            .unwrap()
            .execute_raw(
                "ATTACH DATABASE ? AS ?",
                &[
                    ParameterizedValue::from(self.file_path.as_str()),
                    ParameterizedValue::from(db),
                ],
            )
            .unwrap();
    }

    fn detach_database(&self, db: &str) {
        self.inner
            .lock()
            .unwrap()
            .execute_raw("DETACH DATABASE ?", &[ParameterizedValue::from(db)])
            .unwrap();
    }

    fn with_connection<F, T>(&self, db: &str, f: F) -> T
    where
        F: FnOnce(&mut Queryable) -> T,
    {
        self.attach_database(db);

        let res = {
            let mut conn = self.inner.lock().unwrap();
            f(&mut **conn)
        };

        self.detach_database(db);

        res
    }
}

impl MigrationDatabase for Sqlite {
    fn execute(&self, db: &str, q: Query) -> prisma_query::Result<Option<Id>> {
        self.with_connection(db, |conn| conn.execute(q))
    }

    fn query(&self, db: &str, q: Query) -> prisma_query::Result<ResultSet> {
        self.with_connection(db, |conn| conn.query(q))
    }

    fn query_raw(&self, db: &str, sql: &str, params: &[ParameterizedValue]) -> prisma_query::Result<ResultSet> {
        self.with_connection(db, |conn| conn.query_raw(sql, params))
    }

    fn execute_raw(&self, db: &str, sql: &str, params: &[ParameterizedValue]) -> prisma_query::Result<u64> {
        self.with_connection(db, |conn| conn.execute_raw(sql, params))
    }
}

pub struct PostgreSql {
    inner: Mutex<Box<dyn Queryable>>,
}

impl PostgreSql {
    pub fn new(params: PostgresParams) -> prisma_query::Result<Self> {
        let conn = connector::PostgreSql::new(params.config, Some(params.schema.clone()))?;

        Ok(Self {
            inner: Mutex::new(Box::new(conn)),
        })
    }
}

impl MigrationDatabase for PostgreSql {
    fn execute(&self, _: &str, q: Query) -> prisma_query::Result<Option<Id>> {
        self.inner.lock().unwrap().execute(q)
    }

    fn query(&self, _: &str, q: Query) -> prisma_query::Result<ResultSet> {
        self.inner.lock().unwrap().query(q)
    }

    fn query_raw(&self, _: &str, sql: &str, params: &[ParameterizedValue]) -> prisma_query::Result<ResultSet> {
        self.inner.lock().unwrap().query_raw(sql, params)
    }

    fn execute_raw(&self, _: &str, sql: &str, params: &[ParameterizedValue]) -> prisma_query::Result<u64> {
        self.inner.lock().unwrap().execute_raw(sql, params)
    }
}

pub struct Mysql {
    inner: Mutex<Box<dyn Queryable>>,
}

impl Mysql {
    pub fn new(params: MysqlParams) -> prisma_query::Result<Self> {
        let conn = connector::Mysql::new(params.config)?;

        Ok(Self {
            inner: Mutex::new(Box::new(conn)),
        })
    }
}

impl MigrationDatabase for Mysql {
    fn execute(&self, _: &str, q: Query) -> prisma_query::Result<Option<Id>> {
        self.inner.lock().unwrap().execute(q)
    }

    fn query(&self, _: &str, q: Query) -> prisma_query::Result<ResultSet> {
        self.inner.lock().unwrap().query(q)
    }

    fn query_raw(&self, _: &str, sql: &str, params: &[ParameterizedValue]) -> prisma_query::Result<ResultSet> {
        self.inner.lock().unwrap().query_raw(sql, params)
    }

    fn execute_raw(&self, _: &str, sql: &str, params: &[ParameterizedValue]) -> prisma_query::Result<u64> {
        self.inner.lock().unwrap().execute_raw(sql, params)
    }
}
