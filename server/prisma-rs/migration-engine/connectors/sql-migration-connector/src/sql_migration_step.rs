
pub enum SqlMigrationStep {
    CreateTable(CreateTable),
    AlterTable(AlterTable),
    DropTable(DropTable),
}
pub struct CreateTable {
    pub name: String, 
    pub columns: Vec<ColumnDescription>
}

pub struct DropTable {
    pub name: String
}

pub struct AlterTable {
    pub table: String, 
    pub changes: Vec<TableChange>
}

pub enum TableChange {
    AddColumn(AddColumn),
    AlterColumn(AlterColumn),
    DropColumn(DropColumn),
}

pub struct AddColumn { 
    pub column: ColumnDescription
}
pub struct DropColumn { 
    pub name: String
}

pub struct AlterColumn { 
    pub name: String, 
    pub column: ColumnDescription
}

pub struct ColumnDescription {
    pub name: String,
    pub tpe: Box<ColumnType>,
    pub required: bool,
}

pub trait ColumnType {
    fn render(&self) -> String;
}
