use crate::SqlMigrationStep;
use migration_connector::*;
use prisma_query::error::Error as SqlError;

pub struct SqlDestructiveChangesChecker {}

#[allow(unused, dead_code)]
impl DestructiveChangesChecker<SqlMigrationStep> for SqlDestructiveChangesChecker {
    type ErrorType = SqlError;

    fn check(&self, steps: Vec<SqlMigrationStep>) -> Result<Vec<MigrationResult>, SqlError> {
        Ok(vec![])
    }
}
