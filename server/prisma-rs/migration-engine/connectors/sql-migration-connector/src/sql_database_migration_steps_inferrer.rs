use crate::SqlMigrationStep;
use datamodel::Schema;
use migration_connector::*;

pub struct SqlDatabaseMigrationStepsInferrer {}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepsInferrer<SqlMigrationStep> for SqlDatabaseMigrationStepsInferrer {
    fn infer(&self, previous: &Schema, next: &Schema, steps: Vec<MigrationStep>) -> Vec<SqlMigrationStep> {
        vec![]
    }
}
