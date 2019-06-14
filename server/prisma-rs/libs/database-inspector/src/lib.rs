#![allow(dead_code)]

mod database_inspector_impl;
mod empty_impl;
mod information_schema;
mod postgres_inspector;
mod sqlite;

pub use database_inspector_impl::*;
pub use empty_impl::*;
use sqlite::Sqlite;
use postgres_inspector::Postgres;
use prisma_query::connector::{Sqlite as SqliteDriver, PostgreSql as PostgresDriver };
use prisma_query::Connectional;
use std::sync::Arc;
use url::Url;
use std::borrow::Cow;
use postgres::Config as PostgresConfig;
use std::time::Duration;

pub trait DatabaseInspector {
    fn introspect(&self, schema: &String) -> DatabaseSchema;
}

impl DatabaseInspector {
    pub fn empty() -> EmptyDatabaseInspectorImpl {
        EmptyDatabaseInspectorImpl {}
    }

    pub fn sqlite(file_path: String) -> Sqlite {
        let connection_limit = 5;
        let test_mode = false;        
        let conn = Arc::new(SqliteDriver::new(file_path, connection_limit, test_mode).unwrap());
        Self::sqlite_with_connectional(conn)
    }

    pub fn sqlite_with_connectional(connectional: Arc<Connectional>) -> Sqlite {
        Sqlite::new(connectional)
    }

    pub fn postgres(url: String) -> Postgres {
        // TODO: this needs to move into prisma-query
        // TODO: the setup calls in here look wrong
        let parsed_url = Url::parse(&url).expect("Parsing of the provided connector url failed.");
        let connection_limit = 5;
        let mut config = PostgresConfig::new();
        if let Some(host) = parsed_url.host_str() {
            config.host(host);
        }
        config.user(parsed_url.username());
        if let Some(password) = parsed_url.password() {
            config.password(password);
        }
        let mut db_name = parsed_url.path().to_string();
        db_name.replace_range(..1, ""); // strip leading slash
        config.connect_timeout(Duration::from_secs(5));

        let root_connection = Arc::new(PostgresDriver::new(config.clone(), 1).unwrap());
        let db_sql = format!("CREATE DATABASE \"{}\";", &db_name);
        let schema_name = parsed_url.query_pairs().into_iter().find(|qp| qp.0 == Cow::Borrowed("schema")).expect("schema param is missing").1.to_string();
        let _ = root_connection.query_on_raw_connection(&schema_name, &db_sql, &[]); // ignoring errors as there's no CREATE DATABASE IF NOT EXISTS in Postgres


        config.dbname(&db_name);

        let schema_connection = Arc::new(PostgresDriver::new(config, connection_limit).unwrap());
        let schema_sql = dbg!(format!("CREATE SCHEMA IF NOT EXISTS \"{}\";", &schema_name));                
        schema_connection.query_on_raw_connection(&schema_name, &schema_sql, &[]).expect("Creation of Postgres Schema failed");
        Postgres::new(schema_connection)
    }

    pub fn postgres_with_connectional(connectional: Arc<Connectional>) -> Postgres {
        Postgres::new(connectional)
    }
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct DatabaseSchema {
    pub tables: Vec<Table>,
}

impl DatabaseSchema {
    pub fn table(&self, name: &str) -> Result<&Table, String> {
        match self.tables.iter().find(|t| t.name == name) {
            Some(t) => Ok(t),
            None => Err(format!("Table {} not found", name))
        }
    }

    pub fn table_bang(&self, name: &str) -> &Table {
        self.table(&name).unwrap()
    }

    pub fn has_table(&self, name: &str) -> bool {
        self.table(name).is_ok()
    }

    pub fn empty() -> DatabaseSchema {
        DatabaseSchema { tables: Vec::new() }
    }
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct Table {
    pub name: String,
    pub columns: Vec<Column>,
    pub indexes: Vec<Index>,
    pub primary_key_columns: Vec<String>,
}

impl Table {
    pub fn column_bang(&self, name: &str) -> &Column {
        self.column(name)
            .expect(&format!("Column {} not found in Table {}", name, self.name))
    }

    pub fn column(&self, name: &str) -> Option<&Column> {
        self.columns.iter().find(|c| c.name == name)
    }

    pub fn has_column(&self, name: &str) -> bool {
        self.column(name).is_some()
    }
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct Column {
    pub name: String,
    pub tpe: ColumnType,
    pub is_required: bool,
    pub foreign_key: Option<ForeignKey>,
    pub sequence: Option<Sequence>,
}

impl Column {
    pub fn new(name: String, tpe: ColumnType, is_required: bool) -> Column {
        Column {
            name,
            tpe,
            is_required,
            foreign_key: None,
            sequence: None,
        }
    }

    pub fn with_foreign_key(name: String, tpe: ColumnType, is_required: bool, foreign_key: ForeignKey) -> Column {
        Column {
            name,
            tpe,
            is_required,
            foreign_key: Some(foreign_key),
            sequence: None,
        }
    }
}

#[derive(Debug, Copy, PartialEq, Eq, Clone)]
pub enum ColumnType {
    Int,
    Float,
    Boolean,
    String,
    DateTime,
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct ForeignKey {
    pub table: String,
    pub column: String,
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct Sequence {
    pub name: String,
    pub current: u32,
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct Index {
    pub name: String,
    pub columns: Vec<String>,
    pub unique: bool,
}
