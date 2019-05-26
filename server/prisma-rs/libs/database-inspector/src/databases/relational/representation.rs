#[derive(Clone, PartialEq, Debug)]
pub struct InternalIndexIntrospectionResult {
    pub table_name: String,
    pub name: String,
    pub columns: Vec<String>,
    pub is_unique: bool,
    pub is_primary_key: bool,
}

#[derive(Clone, PartialEq, Debug)]
pub struct EnumInfo {
    pub name: String,
    pub values: Vec<String>,
}

#[derive(Clone, PartialEq, Debug)]
pub struct TableInfo {
    pub name: String,
    pub columns: Vec<ColumnInfo>,
    pub indexes: Vec<IndexInfo>,
    pub primary_key: Option<IndexInfo>,
}

impl TableInfo {
    pub fn column(&self, name: &str) -> Option<&ColumnInfo> {
        self.columns.iter().find(|c| c.name == name)
    }

    pub fn has_column(&self, name: &str) -> bool {
        self.column(name).is_some()
    }
}

#[derive(Clone, PartialEq, Debug)]
pub struct ColumnInfo {
    pub name: String,
    pub column_type: ColumnType,
    pub default_value: Option<String>,
    pub is_unique: bool, // TODO: maybe remove here
    pub comment: Option<String>,
    pub is_nullable: bool,
    pub is_list: bool,
    pub is_auto_increment: bool,
    pub is_primary_key: bool,
}

impl ColumnInfo {
    pub fn new(name: &str, column_type: ColumnType, is_nullable: bool) -> ColumnInfo {
        ColumnInfo {
            name: String::from(name),
            column_type: column_type,
            default_value: None,
            is_unique: false,
            comment: None,
            is_nullable: is_nullable,
            is_list: false,
            is_auto_increment: false,
            is_primary_key: false
        }
    }
}

#[derive(Clone, PartialEq, Debug)]
pub struct SequenceInfo {
    pub name: String,
    pub initial_value: u32,
    pub allocation_vize: u32,
}

#[derive(Clone, PartialEq, Debug)]
pub struct TableRelationInfo {
    /// Table that EMBEDS the foreign key.
    pub source_table: String,
    /// Table that HOLDS the primary key.
    pub target_table: String,
    pub source_column: String,
    pub target_column: String,
}

impl TableRelationInfo {
    pub fn new(source_table: &str, source_column: &str, target_table: &str, target_column: &str) -> TableRelationInfo {
        TableRelationInfo {
            source_table: String::from(source_table),
            source_column: String::from(source_column),
            target_table: String::from(target_table),
            target_column: String::from(target_column),
        }
    }
}

#[derive(Clone, PartialEq, Debug)]
pub struct IndexInfo {
    pub name: String,
    pub columns: Vec<String>,
    pub is_unique: bool,
}

impl IndexInfo {
    pub fn new_pk(cols: &[&str]) -> IndexInfo {
        IndexInfo {
            name: String::from(""),
            columns: cols.iter().map(|x| String::from(*x)).collect(),
            is_unique: true
        }
    }

    pub fn new_unique(name: &str, cols: &[&str]) -> IndexInfo {
        IndexInfo {
            name: String::from(name),
            columns: cols.iter().map(|x| String::from(*x)).collect(),
            is_unique: true
        }
    }
}

#[derive(Clone, PartialEq, Debug)]
pub struct SchemaInfo {
    pub tables: Vec<TableInfo>,
    pub relations: Vec<TableRelationInfo>,
    pub enums: Vec<EnumInfo>,
    pub sequences: Vec<SequenceInfo>
}

impl SchemaInfo {
    pub fn new() -> SchemaInfo {
        SchemaInfo {
            tables: vec![],
            relations: vec![],
            enums: vec![],
            sequences: vec![]
        }
    }

    pub fn merge(&mut self, mut other: SchemaInfo) {
        self.tables.append(&mut other.tables);
        self.relations.append(&mut other.relations);
        self.enums.append(&mut other.enums);
        self.sequences.append(&mut other.sequences);
    }

    pub fn table(&self, name: &str) -> Option<&TableInfo> {
        self.tables.iter().find(|t| t.name == name)
    }

    pub fn has_table(&self, name: &str) -> bool {
        self.table(name).is_some()
    }
}

impl From<InternalIndexIntrospectionResult> for IndexInfo {
    fn from(idx: InternalIndexIntrospectionResult) -> Self {
        IndexInfo {
            columns: idx.columns,
            is_unique: idx.is_unique,
            name: idx.name,
        }
    }
}

#[derive(Debug, Copy, PartialEq, Eq, Clone, serde::Serialize)]
pub enum ColumnType {
    Int,
    Float,
    Boolean,
    String,
    DateTime
}