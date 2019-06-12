#![allow(dead_code)]

mod database_inspector_impl;
mod empty_impl;
mod information_schema;
mod postgres;
mod sqlite;

pub use database_inspector_impl::*;
pub use empty_impl::*;
use sqlite::Sqlite;
use prisma_query::connector::Sqlite as SqliteDatabaseClient;
use prisma_query::Connectional;
use std::sync::Arc;

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
        let conn = std::sync::Arc::new(SqliteDatabaseClient::new(file_path, connection_limit, test_mode).unwrap());
        Self::sqlite_with_connectional(conn)
    }

    pub fn sqlite_with_connectional(connectional: Arc<Connectional>) -> Sqlite {
        Sqlite::new(connectional)
    }
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct DatabaseSchema {
    pub tables: Vec<Table>,
}

impl DatabaseSchema {
    pub fn table(&self, name: &str) -> Option<&Table> {
        self.tables.iter().find(|t| t.name == name)
    }

    pub fn table_bang(&self, name: &str) -> &Table {
        self.table(&name).expect(&format!("Table {} not found", name))
    }

    pub fn has_table(&self, name: &str) -> bool {
        self.table(name).is_some()
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
