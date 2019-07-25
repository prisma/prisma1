use failure::{Error, Fail};

pub mod sqlite;

/// Introspection errors.
#[derive(Debug, Fail)]
pub enum IntrospectionError {
    /// An unknown error occurred.
    #[fail(display = "unknown")]
    UnknownError,
}

/// The result type.
pub type Result<T> = core::result::Result<T, Error>;

/// A database introspection connector.
pub trait IntrospectionConnector {
    /// List the database's schemas.
    fn list_schemas(&self) -> Result<Vec<String>>;
    /// Introspect a database schema.
    fn introspect(&mut self, schema: &str) -> Result<DatabaseSchema>;
}

/// The result of introspecting a database schema.
pub struct DatabaseSchema {
    /// The schema's tables.
    pub tables: Vec<Table>,
    /// The schema's enums.
    pub enums: Vec<Enum>,
    /// The schema's sequences, unique to Postgres.
    pub sequences: Vec<Sequence>,
}

impl DatabaseSchema {
    /// Get a table.
    pub fn table(&self, name: &str) -> Option<&Table> {
        self.tables.iter().find(|x| x.name == name)
    }
}

/// A table found in a schema.
pub struct Table {
    /// The table's name.
    pub name: String,
    /// The table's columns.
    pub columns: Vec<Column>,
    /// The table's indices.
    pub indexes: Vec<Index>,
    /// The table's primary key, if there is one.
    pub primary_key: Option<PrimaryKey>,
    /// The table's foreign keys.
    pub foreign_keys: Vec<ForeignKey>,
}

/// An index of a table.
#[derive(PartialEq, Debug)]
pub struct Index {
    /// Index name.
    pub name: String,
    /// Index columns.
    pub columns: Vec<String>,
    /// Is index unique?
    pub unique: bool,
}

/// The primary key of a table.
#[derive(PartialEq, Debug)]
pub struct PrimaryKey {
    /// Columns.
    pub columns: Vec<String>,
}

/// A column of a table.
#[derive(PartialEq, Clone, Debug)]
pub struct Column {
    /// Column name.
    pub name: String,
    /// Column type.
    pub tpe: ColumnType,
    /// Column arity.
    pub arity: ColumnArity,
    /// Column default.
    pub default: Option<String>, // does this field need to be richer? E.g. to easier detect the usages of sequences here
    /// Column auto increment setting, MySQL/SQLite only.
    pub auto_increment: Option<bool>,
}

/// The type of a column.
#[derive(PartialEq, Clone, Debug)]
pub struct ColumnType {
    /// The raw SQL type.
    pub raw: String,
    /// The family of the raw type.
    pub family: ColumnTypeFamily,
}

/// Enumeration of column type families.
#[derive(PartialEq, Clone, Debug)]
// TODO: this name feels weird.
pub enum ColumnTypeFamily {
    /// Integer types.
    Int,
    /// Floating point types.
    Float,
    /// Boolean types.
    Boolean,
    /// String types.
    String,
    /// DateTime types.
    DateTime,
    /// Double precision floating point types.
    Double,
    /// Binary types.
    Binary,
    /// Binary array types.
    BinArray,
    /// Bool array types.
    BoolArray,
    /// DateTime types.
    DateTimeArray,
    /// Double precision floating point array types.
    DoubleArray,
    /// Floating point array types.
    FloatArray,
    /// Integer array types.
    IntArray,
    /// String array types.
    StringArray,
}

/// A column's arity.
#[derive(PartialEq, Clone, Debug)]
pub enum ColumnArity {
    /// Required column.
    Required,
    /// Nullable column.
    Nullable,
    /// List type column.
    List,
}

/// A foreign key.
#[derive(PartialEq, Debug)]
pub struct ForeignKey {
    /// Foreign key name.
    pub name: Option<String>,
    /// Foreign source columns.
    pub source_columns: Vec<String>,
    /// Foreign target table.
    pub target_table: String,
    /// Foreign key target columns.
    pub target_columns: Vec<String>,
}

/// A SQL enum.
pub struct Enum {
    /// Enum name.
    pub name: String,
    /// Possible enum values.
    pub values: Vec<String>,
}

/// A SQL sequence.
pub struct Sequence {
    /// Sequence name.
    pub name: String,
    /// Sequence initial value.
    pub initial_value: u32,
    /// Sequence allocation size.
    pub allocation_size: u32,
}
