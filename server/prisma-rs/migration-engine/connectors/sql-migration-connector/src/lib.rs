mod sql_migration_persistence;
mod sql_database_migration_steps_inferrer;
mod sql_database_step_applier;
mod sql_destructive_changes_checker;

use sql_migration_persistence::*;
use sql_database_migration_steps_inferrer::*;
use sql_database_step_applier::*;
use sql_destructive_changes_checker::*;
use migration_connector::*;
use std::sync::Arc;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector {
    migration_persistence: Arc<MigrationPersistence>,
    sql_database_migration_steps_inferrer: Arc<DatabaseMigrationStepsInferrer<SqlMigrationStep>>,
    database_step_applier: Arc<DatabaseMigrationStepApplier<SqlMigrationStep>>,
    destructive_changes_checker: Arc<DestructiveChangesChecker<SqlMigrationStep>>,
}

impl SqlMigrationConnector {
    // FIXME: this must take the config as a param at some point
    pub fn new() -> SqlMigrationConnector {
        let migration_persistence = Arc::new(SqlMigrationPersistence{});
        let sql_database_migration_steps_inferrer = Arc::new(SqlDatabaseMigrationStepsInferrer{});
        let database_step_applier = Arc::new(SqlDatabaseStepApplier{});
        let destructive_changes_checker = Arc::new(SqlDestructiveChangesChecker{});
        SqlMigrationConnector{migration_persistence, sql_database_migration_steps_inferrer, database_step_applier, destructive_changes_checker}
    }
}

impl MigrationConnector for SqlMigrationConnector {
    type DatabaseMigrationStep = SqlMigrationStep;

    fn migration_persistence(&self) -> Arc<MigrationPersistence> {
        Arc::clone(&self.migration_persistence)
    }

    fn database_steps_inferrer(&self) -> Arc<DatabaseMigrationStepsInferrer<SqlMigrationStep>> {
        Arc::clone(&self.sql_database_migration_steps_inferrer)
    }

    fn database_step_applier(&self) -> Arc<DatabaseMigrationStepApplier<SqlMigrationStep>> {
        Arc::clone(&self.database_step_applier)
    }

    fn destructive_changes_checker(&self) -> Arc<DestructiveChangesChecker<SqlMigrationStep>> {
        Arc::clone(&self.destructive_changes_checker)
    }
}

pub enum SqlMigrationStep {
    CreateTable,
}