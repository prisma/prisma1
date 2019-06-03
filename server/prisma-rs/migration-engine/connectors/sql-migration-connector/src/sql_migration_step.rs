use migration_connector::DatabaseMigrationMarker;
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct SqlMigration {
    pub steps: Vec<SqlMigrationStep>,
}

impl DatabaseMigrationMarker for SqlMigration {}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum SqlMigrationStep {
    CreateTable(CreateTable),
    AlterTable(AlterTable),
    DropTable(DropTable),
    RenameTable { name: String, new_name: String },
    RawSql { raw: String },
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct CreateTable {
    pub name: String,
    pub columns: Vec<ColumnDescription>,
    pub primary_columns: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DropTable {
    pub name: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AlterTable {
    pub table: String,
    pub changes: Vec<TableChange>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum TableChange {
    AddColumn(AddColumn),
    AlterColumn(AlterColumn),
    DropColumn(DropColumn),
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AddColumn {
    pub column: ColumnDescription,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DropColumn {
    pub name: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AlterColumn {
    pub name: String,
    pub column: ColumnDescription,
}

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq)]
pub struct ColumnDescription {
    pub name: String,
    pub tpe: ColumnType,
    pub required: bool,
    pub foreign_key: Option<ForeignKey>,
}

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq)]
pub struct ForeignKey {
    pub table: String,
    pub column: String,
}

#[derive(Debug, Copy, PartialEq, Eq, Clone, Serialize, Deserialize)]
pub enum ColumnType {
    Int,
    Float,
    Boolean,
    String,
    DateTime,
}
