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

fn get_table_names(schema: &String) -> Vec<String> {
    let sql: &'static str = "
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
    let cols = get_column(&schema, &table);
    let foreign = get_foreign_constraint(&schema, &table);
    let index = get_index(&schema, &table);
    let seq = get_sequence(&schema, &table);

    unimplemented!()
}

fn get_column(schema: &String, table: &String) -> Column {
    unimplemented!()
}

fn get_foreign_constraint(schema: &String, table: &String) -> ForeignKey {
    unimplemented!()
}

fn get_sequence(schema: &String, table: &String) -> Sequence {
    unimplemented!()
}

fn get_index(schema: &String, table: &String) -> Index {
    unimplemented!()
}

pub struct DatabaseSchema {
    pub tables: Vec<Table>,
}

impl DatabaseSchema {
    pub fn table(&self, name: &str) -> Option<&Table> {
        self.tables.iter().find(|t| t.name == name)
    }
}

pub struct Table {
    pub name: String,
    pub columns: Vec<Column>,
    pub indexes: Vec<Index>,
}

pub struct Column {
    pub name: String,
    pub tpe: String,
    pub nullable: bool,
    pub foreign_key: Option<ForeignKey>,
    pub sequence: Option<Sequence>,
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
