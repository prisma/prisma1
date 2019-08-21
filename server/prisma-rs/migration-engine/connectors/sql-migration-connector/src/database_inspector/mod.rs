#![allow(dead_code)]

mod database_inspector_impl;
mod database_schema;
mod empty_impl;
mod information_schema;
mod mysql_inspector;
mod postgres_inspector;
mod sqlite_inspector;

use crate::migration_database::{
    MigrationDatabase, Mysql as MysqlDriver, PostgreSql as PostgresDriver, Sqlite as SqliteDriver,
};
pub use database_inspector_impl::*;
pub use database_schema::*;
pub use empty_impl::*;
use mysql_inspector::MysqlInspector;
use postgres_inspector::Postgres;
use prisma_query::connector;
use sqlite_inspector::Sqlite;
use std::convert::TryFrom;
use std::sync::Arc;
use url::Url;

pub trait DatabaseInspector: Send + Sync + 'static {
    fn introspect(&self, schema: &String) -> DatabaseSchema;
}

impl dyn DatabaseInspector {
    const PARSE_ERROR: &'static str = "Parsing of the provided connector url failed.";

    pub fn empty() -> EmptyDatabaseInspectorImpl {
        EmptyDatabaseInspectorImpl {}
    }

    pub fn sqlite(file_path: String) -> Sqlite {
        let conn = Arc::new(SqliteDriver::new(&file_path).unwrap());
        Self::sqlite_with_database(conn)
    }

    pub fn sqlite_with_database(database: Arc<dyn MigrationDatabase + Send + Sync + 'static>) -> Sqlite {
        Sqlite::new(database)
    }

    pub fn postgres(url: String) -> Postgres {
        let url = Url::parse(&url).expect(Self::PARSE_ERROR);
        let db_name = url.path().trim_start_matches("/");

        let mut root_url = url.clone();
        root_url.set_path("postgres");

        let root_params = connector::PostgresParams::try_from(root_url).expect(Self::PARSE_ERROR);

        let schema_name = root_params.schema.clone();
        let root_connection = Arc::new(PostgresDriver::new(root_params).unwrap());

        let db_sql = format!("CREATE DATABASE \"{}\";", &db_name);
        debug!("{}", db_sql);

        let _ = root_connection.query_raw(&schema_name, &db_sql, &[]); // ignoring errors as there's no CREATE DATABASE IF NOT EXISTS in Postgres

        let params = connector::PostgresParams::try_from(url).expect(Self::PARSE_ERROR);
        let schema_connection = Arc::new(PostgresDriver::new(params).unwrap());

        let schema_sql = format!("CREATE SCHEMA IF NOT EXISTS \"{}\";", &schema_name);
        debug!("{}", schema_sql);

        schema_connection
            .query_raw(&schema_name, &schema_sql, &[])
            .expect("Creation of Postgres Schema failed");

        Postgres::new(schema_connection)
    }

    pub fn postgres_with_database(database: Arc<dyn MigrationDatabase + Send + Sync + 'static>) -> Postgres {
        Postgres::new(database)
    }

    pub fn mysql(url: String) -> MysqlInspector {
        let url = Url::parse(&url).expect(Self::PARSE_ERROR);
        let params = connector::MysqlParams::try_from(url).expect(Self::PARSE_ERROR);
        let database = MysqlDriver::new(params).unwrap();

        Self::mysql_with_database(Arc::new(database))
    }

    pub fn mysql_with_database(database: Arc<dyn MigrationDatabase + Send + Sync + 'static>) -> MysqlInspector {
        MysqlInspector::new(database)
    }
}
