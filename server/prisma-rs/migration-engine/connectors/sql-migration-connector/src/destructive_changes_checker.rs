use crate::SqlMigrationStep;
use migration_connector::*;
use prisma_query::error::Error as SqlError;

pub struct SqlDestructiveChangesChecker {}

impl SqlDestructiveChangesChecker {
    pub fn new() -> SqlDestructiveChangesChecker {
        SqlDestructiveChangesChecker {}
    }
}

#[allow(unused, dead_code)]
impl DestructiveChangesChecker<SqlMigrationStep> for SqlDestructiveChangesChecker {
    fn check(&self, steps: Vec<SqlMigrationStep>) -> Result<Vec<MigrationResult>, SqlError> {
        Ok(vec![])
    }
}
