use crate::SqlMigrationStep;
use migration_connector::*;

pub struct SqlDestructiveChangesChecker {}

#[allow(unused, dead_code)]
impl DestructiveChangesChecker<SqlMigrationStep> for SqlDestructiveChangesChecker {
    fn check(&self, steps: &Vec<SqlMigrationStep>) -> Vec<MigrationResult> {
        vec![]
    }
}
