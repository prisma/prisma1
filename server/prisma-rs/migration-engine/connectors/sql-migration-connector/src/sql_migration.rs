use database_introspection::*;
use datamodel::Value;
use migration_connector::DatabaseMigrationMarker;
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct SqlMigration {
    pub before: DatabaseSchema,
    pub after: DatabaseSchema,
    pub steps: Vec<SqlMigrationStep>,
    pub rollback: Vec<SqlMigrationStep>,
}

impl SqlMigration {
    pub fn empty() -> SqlMigration {
        SqlMigration {
            before: DatabaseSchema::empty(),
            after: DatabaseSchema::empty(),
            steps: Vec::new(),
            rollback: Vec::new(),
        }
    }
}

impl DatabaseMigrationMarker for SqlMigration {
    fn serialize(&self) -> serde_json::Value {
        serde_json::to_value(self).unwrap()
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum SqlMigrationStep {
    CreateTable(CreateTable),
    AlterTable(AlterTable),
    DropTable(DropTable),
    DropTables(DropTables),
    RenameTable { name: String, new_name: String },
    RawSql { raw: String },
    CreateIndex(CreateIndex),
    DropIndex(DropIndex),
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct CreateTable {
    pub table: Table,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DropTable {
    pub name: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DropTables {
    pub names: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AlterTable {
    pub table: Table,
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
    pub column: Column,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DropColumn {
    pub name: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AlterColumn {
    pub name: String,
    pub column: Column,
}

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq)]
pub struct CreateIndex {
    pub table: String,
    pub index: Index
}

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq)]
pub struct DropIndex {
    pub table: String,
    pub name: String,
}
