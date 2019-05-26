use crate::SqlMigrationStep;
use migration_connector::*;
use prisma_query::{transaction::Connection, error::Error as SqlError};

pub struct SqlDestructiveChangesChecker {}

#[allow(unused, dead_code)]
impl DestructiveChangesChecker<SqlMigrationStep> for SqlDestructiveChangesChecker {
    type ErrorType = SqlError;
    type ConnectionType = &'static mut Connection;

    fn check(&self, connection: &mut Connection, steps: Vec<SqlMigrationStep>) -> Result<Vec<MigrationResult>, SqlError> {
        Ok(vec![])
    }
}
