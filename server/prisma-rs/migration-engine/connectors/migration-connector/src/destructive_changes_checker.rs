pub trait DestructiveChangesChecker<T> {
    fn check(&self, database_migration: &T) -> Vec<MigrationErrorOrWarning>;
}

pub enum MigrationErrorOrWarning {
    Error(MigrationWarning),
    Warning(MigrationError),
}

#[derive(Debug, Serialize)]
pub struct MigrationWarning {
    pub tpe: String,
    pub description: String,
    pub field: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct MigrationError {
    pub tpe: String,
    pub description: String,
    pub field: Option<String>,
}
