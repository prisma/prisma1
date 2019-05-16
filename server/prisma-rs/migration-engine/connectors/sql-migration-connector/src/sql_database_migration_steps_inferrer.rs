use crate::database_schema_calculator::DatabaseSchemaCalculator;
use crate::database_schema_differ::DatabaseSchemaDiffer;
use crate::sql_migration_step::*;
use database_inspector::DatabaseInspector;
use datamodel::*;
use migration_connector::steps::*;
use migration_connector::*;

pub struct SqlDatabaseMigrationStepsInferrer {
    pub inspector: Box<DatabaseInspector>,
    pub schema_name: String,
}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepsInferrer<SqlMigrationStep> for SqlDatabaseMigrationStepsInferrer {
    fn infer(&self, previous: &Schema, next: &Schema, steps: Vec<MigrationStep>) -> Vec<SqlMigrationStep> {
        let current_database_schema = self.inspector.introspect(&self.schema_name);
        let expected_database_schema = DatabaseSchemaCalculator::calculate(next);
        let steps = DatabaseSchemaDiffer::diff(current_database_schema, expected_database_schema);
        steps
    }
}

pub fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<SqlMigrationStep>
where
    F: FnMut(T) -> SqlMigrationStep,
{
    steps.into_iter().map(|x| wrap_fn(x)).collect()
}
