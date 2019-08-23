use crate::SqlMigration;
use migration_connector::*;

pub struct SqlDestructiveChangesChecker {}

#[allow(unused, dead_code)]
impl DestructiveChangesChecker<SqlMigration> for SqlDestructiveChangesChecker {
    fn check(&self, database_migration: &SqlMigration) -> Vec<MigrationErrorOrWarning> {
        vec![]
    }
}
