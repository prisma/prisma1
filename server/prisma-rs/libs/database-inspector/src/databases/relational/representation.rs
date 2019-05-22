#[derive(Clone)]
pub struct InternalIndexIntrospectionResult {
    pub table_name: String,
    pub name: String,
    pub fields: Vec<String>,
    pub is_unique: bool,
    pub is_primary_key: bool,
}

pub struct EnumInfo {
    pub name: String,
    pub values: Vec<String>,
}

pub struct TableInfo {
    pub name: String,
    pub columns: Vec<ColumnInfo>,
    pub inidices: Vec<IndexInfo>,
    pub primary_key: Option<IndexInfo>,
}

pub struct ColumnInfo {
    pub name: String,
    pub is_unique: bool,
    pub default_alue: Option<String>,
    pub column_type: String,
    pub comment: Option<String>,
    pub is_nullable: bool,
    pub is_list: bool,
    pub is_auto_increment: bool,
}

pub struct SequenceInfo {
    pub name: String,
    pub initial_value: u32,
    pub allocation_vize: u32,
}

pub struct TableRelationInfo {
    pub source_table: String,
    pub target_table: String,
    pub source_column: String,
    pub target_column: String,
}

#[derive(Clone)]
pub struct IndexInfo {
    pub name: String,
    pub fields: Vec<String>,
    pub is_unique: bool,
}

impl From<InternalIndexIntrospectionResult> for IndexInfo {
    fn from(idx: InternalIndexIntrospectionResult) -> Self {
        IndexInfo {
            fields: idx.fields,
            is_unique: idx.is_unique,
            name: idx.name,
        }
    }
}
