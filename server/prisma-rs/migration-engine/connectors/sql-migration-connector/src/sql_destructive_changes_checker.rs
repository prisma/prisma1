use migration_connector::*;
use crate::SqlMigrationStep;

pub struct SqlDestructiveChangesChecker {}


#[allow(unused, dead_code)]
impl DestructiveChangesChecker<SqlMigrationStep> for SqlDestructiveChangesChecker {
    fn check(&self, steps: Vec<SqlMigrationStep>) -> Vec<MigrationResult> {
        vec![]
    }
}