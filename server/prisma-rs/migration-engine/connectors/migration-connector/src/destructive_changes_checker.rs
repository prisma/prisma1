use std::marker::PhantomData;

pub trait DestructiveChangesChecker<T>: Send + Sync + 'static
where
    T: Send + Sync + 'static,
{
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

pub struct EmptyDestructiveChangesChecker<T> {
    database_migration: PhantomData<T>,
}

impl<T> EmptyDestructiveChangesChecker<T> {
    pub fn new() -> EmptyDestructiveChangesChecker<T> {
        EmptyDestructiveChangesChecker {
            database_migration: PhantomData,
        }
    }
}

impl<T> DestructiveChangesChecker<T> for EmptyDestructiveChangesChecker<T>
where
    T: Send + Sync + 'static,
{
    fn check(&self, _database_migration: &T) -> Vec<MigrationErrorOrWarning> {
        Vec::new()
    }
}
