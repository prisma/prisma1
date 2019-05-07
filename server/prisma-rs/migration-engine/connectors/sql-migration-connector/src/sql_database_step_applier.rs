use crate::SqlMigrationStep;
use migration_connector::*;

pub struct SqlDatabaseStepApplier {}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepApplier<SqlMigrationStep> for SqlDatabaseStepApplier {
    fn apply(&self, step: SqlMigrationStep) {}
}
