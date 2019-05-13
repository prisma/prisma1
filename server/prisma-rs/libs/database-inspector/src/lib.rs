pub trait DatabaseInspector {
    fn inspect(schema: String) -> DatabaseSchema;
}

pub struct EmptyDatabaseInspectorImpl;

impl DatabaseInspector for EmptyDatabaseInspectorImpl {
    fn inspect(schema: String) -> DatabaseSchema {
        DatabaseSchema {
            tables: get_table_names(&schema)
                .into_iter()
                .map(|t| get_table(&schema, &t))
                .collect(),
        }
    }
}

fn get_table_names(_schema: &String) -> Vec<String> {
    let _sql: &'static str = "
SELECT
    table_name
FROM
    information_schema.tables
WHERE
    table_schema = $schema AND
    -- Views are not supported yet
    table_type = 'BASE TABLE'
    ";

    vec![]
}

fn get_table(schema: &String, table: &String) -> Table {
    let _cols = get_column(&schema, &table);
    let _foreign = get_foreign_constraint(&schema, &table);
    let _index = get_index(&schema, &table);
    let _seq = get_sequence(&schema, &table);

    unimplemented!()
}

fn get_column(_schema: &String, _table: &String) -> Column {
    unimplemented!()
}

fn get_foreign_constraint(_schema: &String, _table: &String) -> ForeignKey {
    unimplemented!()
}

fn get_sequence(_schema: &String, _table: &String) -> Sequence {
    unimplemented!()
}

fn get_index(_schema: &String, _table: &String) -> Index {
    unimplemented!()
}

pub struct DatabaseSchema {
    pub tables: Vec<Table>,
}

impl DatabaseSchema {
    pub fn table(&self, name: &str) -> Option<&Table> {
        self.tables.iter().find(|t| t.name == name)
    }

    pub fn has_table(&self, name: &str) -> bool {
        self.table(name).is_some()
    }
}

pub struct Table {
    pub name: String,
    pub columns: Vec<Column>,
    pub indexes: Vec<Index>,
}

impl Table {
    pub fn column(&self, name: &str) -> Option<&Column> {
        self.columns.iter().find(|c| c.name == name)
    }

    pub fn has_column(&self, name: &str) -> bool {
        self.column(name).is_some()
    }
}

pub struct Column {
    pub name: String,
    pub tpe: ColumnType,
    pub required: bool,
    pub foreign_key: Option<ForeignKey>,
    pub sequence: Option<Sequence>,
}

#[derive(Debug, Copy, PartialEq, Eq, Clone)]
pub enum ColumnType {
    Int,
    Float,
    Boolean,
    String,
    DateTime,
}

pub struct ForeignKey {
    pub table: String,
    pub column: String,
}

pub struct Sequence {
    pub name: String,
    pub current: u32,
}

pub struct Index {
    pub name: String,
    pub columns: Vec<String>,
    pub unique: bool,
}
