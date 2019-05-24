#[derive(Clone, PartialEq)]
pub struct InternalIndexIntrospectionResult {
    pub table_name: String,
    pub name: String,
    pub fields: Vec<String>,
    pub is_unique: bool,
    pub is_primary_key: bool,
}

#[derive(Clone, PartialEq)]
pub struct EnumInfo {
    pub name: String,
    pub values: Vec<String>,
}

#[derive(Clone, PartialEq)]
pub struct TableInfo {
    pub name: String,
    pub columns: Vec<ColumnInfo>,
    pub inidices: Vec<IndexInfo>,
    pub primary_key: Option<IndexInfo>,
}

#[derive(Clone, PartialEq)]
pub struct ColumnInfo {
    pub name: String,
    pub is_unique: bool,
    pub default_value: Option<String>,
    pub column_type: ColumnType,
    pub comment: Option<String>,
    pub is_nullable: bool,
    pub is_list: bool,
    pub is_auto_increment: bool,
    pub is_primary_key: bool,
}

#[derive(Clone, PartialEq)]
pub struct SequenceInfo {
    pub name: String,
    pub initial_value: u32,
    pub allocation_vize: u32,
}

#[derive(Clone, PartialEq)]
pub struct TableRelationInfo {
    /// Table that EMBEDS the foreign key.
    pub source_table: String,
    /// Table that HOLDS the primary key.
    pub target_table: String,
    pub source_column: String,
    pub target_column: String,
}

#[derive(Clone, PartialEq)]
pub struct IndexInfo {
    pub name: String,
    pub fields: Vec<String>,
    pub is_unique: bool,
}

#[derive(Clone, PartialEq)]
pub struct SchemaInfo {
    pub models: Vec<TableInfo>,
    pub relations: Vec<TableRelationInfo>,
    pub enums: Vec<EnumInfo>,
    pub sequences: Vec<SequenceInfo>
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

// TODO: This enum needs to be extended accordingly for more databases.
#[derive(Debug, Copy, PartialEq, Eq, Clone)]
pub enum ColumnType {
    Int,
    Float,
    Boolean,
    String,
    DateTime
}