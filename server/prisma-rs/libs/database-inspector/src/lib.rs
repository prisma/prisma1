pub trait DatabaseInspector {
    fn inspect(schema: String) -> DatabaseSchema;
}

pub struct EmptyDatabaseInspectorImpl;

impl DatabaseInspector for EmptyDatabaseInspectorImpl {
    fn inspect(schema: String) -> DatabaseSchema {
        DatabaseSchema{ tables: vec!() }
    }
}

pub struct DatabaseSchema {
    pub tables: Vec<Table>,
}
pub struct Table {
    pub name: String,
    pub columns: Vec<Column>,
    pub indexes: Vec<Index>,
}

pub struct Column {
    pub name: String,
    pub tpe: String,
    pub type_identifier: String,
    pub foreign_key: Option<ForeignKey>,
    pub sequence: Option<Sequence>,
}

pub struct ForeignKey {
    pub table: String,
    pub column: String,
}

pub struct Sequence {
    pub name: String,
    pub current: u32
}

pub struct Index {
    pub name: String,
    pub columns: Vec<String>,
    pub unique: bool,
}