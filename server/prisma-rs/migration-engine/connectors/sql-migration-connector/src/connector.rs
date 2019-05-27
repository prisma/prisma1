
use database_inspector::relational::{ RelationalIntrospectionResult, RelationalIntrospectionConnector };
use prisma_query::{error::Error as SqlError, transaction::Connection};
use crate::*;
use migration_connector::*;
use std::cell::RefCell;

pub struct SqlMigrationConnector<'a> {
    schema_name: String,
    migration_persistence: SqlMigrationPersistence<'a>,
    sql_database_migration_steps_inferrer: SqlDatabaseMigrationStepsInferrer<'a>,
    database_step_applier: SqlDatabaseStepApplier<'a>,
    destructive_changes_checker: SqlDestructiveChangesChecker,
    applier: SqlMigrationApplier,
    connection: &'a RefCell<Connection>
}


impl<'a> SqlMigrationConnector<'a> {
    pub fn new(schema_name: &str, connection: &'a RefCell<Connection>) -> SqlMigrationConnector<'a> {
        let migration_persistence = SqlMigrationPersistence::new(connection);
        let sql_database_migration_steps_inferrer = SqlDatabaseMigrationStepsInferrer::new(schema_name, connection);
        let database_step_applier = SqlDatabaseStepApplier::new(schema_name, connection);
        let destructive_changes_checker = SqlDestructiveChangesChecker::new();
        let applier = SqlMigrationApplier::new(schema_name);

        SqlMigrationConnector {
            connection,
            schema_name: String::from(schema_name),
            migration_persistence: migration_persistence,
            sql_database_migration_steps_inferrer: sql_database_migration_steps_inferrer,
            database_step_applier: database_step_applier,
            destructive_changes_checker: destructive_changes_checker,
            applier: applier
        }
    }
}

impl<'a> MigrationConnector<SqlMigrationStep> for SqlMigrationConnector<'a> {
    fn initialize(&self) -> Result<(), SqlError> {
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

        dbg!(self.connection.borrow_mut().query_raw(&sql_str, &[]))?;

        Ok(())
    }

    fn reset(&self) -> Result<(), SqlError> {
        let sql_str = format!(r#"DELETE FROM "{}"."_Migration";"#, self.schema_name);

        // This should probably do something.
        Ok(())
    }

    fn migration_persistence(&self) -> &MigrationPersistence {
        &self.migration_persistence
    }

    fn database_steps_inferrer(&self) -> &DatabaseMigrationStepsInferrer<SqlMigrationStep> {
        &self.sql_database_migration_steps_inferrer
    }

    fn database_step_applier(&self) -> &DatabaseMigrationStepApplier<SqlMigrationStep> {
        &self.database_step_applier
    }

    fn destructive_changes_checker(&self) -> &DestructiveChangesChecker<SqlMigrationStep> {
        &self.destructive_changes_checker
    }

    fn migration_applier(&self) -> &MigrationApplier<SqlMigrationStep> {
        &self.applier
    }
}
