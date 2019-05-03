use migration_connector::*;
use crate::SqlMigrationStep;

pub struct SqlDatabaseStepApplier {}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepApplier<SqlMigrationStep> for SqlDatabaseStepApplier {
    fn apply(&self, step: SqlMigrationStep) {

    }
}