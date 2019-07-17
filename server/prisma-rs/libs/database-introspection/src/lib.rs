use failure::{Error, Fail};

pub mod sqlite;

#[derive(Debug, Fail)]
pub enum IntrospectionError {
    #[fail(display = "unknown")]
    UnknownError,
}

pub type Result<T> = core::result::Result<T, Error>;

pub trait IntrospectionConnector {
    fn list_schemas(&self) -> Result<Vec<String>>;
    fn introspect(&self, schema: &str) -> Result<DatabaseSchema>;    
}

pub struct DatabaseSchema {
    pub tables: Vec<Table>,
    pub enums: Vec<Enum>,
    pub sequences: Vec<Sequence>, // sequences are only a thing in Postgres
}

impl DatabaseSchema {
    pub fn table(&self, name: &str) -> Option<&Table> {
        self.tables.iter().find(|x| x.name == name)
    }
}

pub struct Table {
    pub name: String,
    pub columns: Vec<Column>,
    pub indexes: Vec<Index>,
    pub primary_key: Option<Index>,
    pub foreign_keys: Vec<ForeignKey>,
}

pub struct Index {
    pub name: String,
    pub columns: Vec<String>,
    pub unique: bool,
}

#[derive(PartialEq, Clone, Debug)]
pub struct Column {
    pub name: String,
    pub tpe: ColumnType,
    pub arity: ColumnArity,
    pub default: Option<String>, // does this field need to be richer? E.g. to easier detect the usages of sequences here
    pub auto_increment: Option<bool>, // only relevant for MySQL + SQLite
}

#[derive(PartialEq, Clone, Debug)]
pub struct ColumnType {
    pub raw: String, // e.g. varchar(32), mediumtext ...
    pub family: ColumnTypeFamily // the family of the raw type. In this case: String
}

#[derive(PartialEq, Clone, Debug)]
// TODO: this name feels weird.
pub enum ColumnTypeFamily {
    Int,
    Float,
    Boolean,
    String,
    DateTime,
}

#[derive(PartialEq, Clone, Debug)]
pub enum ColumnArity {
    Required,
    Nullable,
    List
}

pub struct ForeignKey {
    pub name: Option<String>,
    pub source_columns: Vec<String>,
    pub target_table: String,
    pub target_columns: Vec<String>,
}

pub struct Enum {
    pub name: String,
    pub values: Vec<String>,
}

pub struct Sequence {
    pub name: String,
    pub initial_value: u32,
    pub allocation_size: u32,
}
