
use database_inspector::relational::{ RelationalIntrospectionResult, RelationalIntrospectionConnector };
use prisma_query::{error::Error as SqlError, transaction::Connection};
use crate::*;
use migration_connector::*;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector<'a> {
    schema_name: String,
    migration_persistence: SqlMigrationPersistence<'a>,
    sql_database_migration_steps_inferrer: SqlDatabaseMigrationStepsInferrer,
    database_step_applier: SqlDatabaseStepApplier<'a>,
    destructive_changes_checker: SqlDestructiveChangesChecker,
    applier: SqlMigrationApplier
}

// TODO FIXME 
impl<'a> SqlMigrationConnector<'a> {
    pub fn new(schema_name: &'a str) -> SqlMigrationConnector<'a> {
        let migration_persistence = SqlMigrationPersistence::new();
        let sql_database_migration_steps_inferrer = SqlDatabaseMigrationStepsInferrer {
            schema_name: String::from(schema_name),
        };
        let database_step_applier = SqlDatabaseStepApplier::new(
            schema_name,
        );
        let destructive_changes_checker = SqlDestructiveChangesChecker {};

        SqlMigrationConnector {
            schema_name: String::from(schema_name),
            migration_persistence,
            sql_database_migration_steps_inferrer,
            database_step_applier,
            destructive_changes_checker,
            applier: SqlMigrationApplier { schema_name: String::from(schema_name) }
        }

    }
}

impl<'a> MigrationConnector<'a> for SqlMigrationConnector<'a> {
    type ConnectionType = &'a mut Connection;
    type ErrorType = SqlError;
    type DatabaseMigrationStep = SqlMigrationStep;
    type MigrationPersistenceType = SqlMigrationPersistence<'a>;
    type MigrationApplierType = SqlMigrationApplier;
    type DatabaseMigrationStepsApplierType = SqlDatabaseStepApplier<'a>;
    type DatabaseMigrationStepsInferrerType = SqlDatabaseMigrationStepsInferrer;
    type DatabaseDestructiveChangesCheckerType = SqlDestructiveChangesChecker;

    fn initialize(&self, connection: &mut Connection) -> Result<(), SqlError> {
        let mut m = barrel::Migration::new().schema(self.schema_name.clone());
        m.create_table_if_not_exists("_Migration", |t| {
            t.add_column("revision", types::primary());
            t.add_column("name", types::text());
            t.add_column("datamodel", types::text());
            t.add_column("status", types::text());
            t.add_column("applied", types::integer());
            t.add_column("rolled_back", types::integer());
            t.add_column("datamodel_steps", types::text());
            t.add_column("database_steps", types::text());
            t.add_column("errors", types::text());
            t.add_column("started_at", types::date());
            t.add_column("finished_at", types::date().nullable(true));
        });

        let sql_str = dbg!(m.make::<Sqlite>());

        dbg!(connection.query_raw(&sql_str, &[]))?;

        Ok(())
    }

    fn reset(&self, connection: &mut Connection) -> Result<(), SqlError> {
        let sql_str = format!(r#"DELETE FROM "{}"."_Migration";"#, self.schema_name);

        Ok(())
    }

    fn migration_persistence(&self) -> &SqlMigrationPersistence<'a> {
        &self.migration_persistence
    }

    fn database_steps_inferrer(&self) -> &SqlDatabaseMigrationStepsInferrer {
        &self.sql_database_migration_steps_inferrer
    }

    fn database_step_applier(&self) -> &SqlDatabaseStepApplier<'a> {
        &self.database_step_applier
    }

    fn destructive_changes_checker(&self) -> &SqlDestructiveChangesChecker {
        &self.destructive_changes_checker
    }

    fn migration_applier(&self) -> &SqlMigrationApplier {
        &self.applier
    }
}
