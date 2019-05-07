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
use rusqlite::{ Connection, NO_PARAMS };
use barrel;
use barrel::types;
use barrel::backend::Sqlite;

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
        let migration_persistence = Arc::new(SqlMigrationPersistence::new(Self::new_conn(SCHEMA_NAME)));
        let sql_database_migration_steps_inferrer = Arc::new(SqlDatabaseMigrationStepsInferrer{});
        let database_step_applier = Arc::new(SqlDatabaseStepApplier{});
        let destructive_changes_checker = Arc::new(SqlDestructiveChangesChecker{});
        SqlMigrationConnector{migration_persistence, sql_database_migration_steps_inferrer, database_step_applier, destructive_changes_checker}
    }

    fn new_conn(name: &str) -> Connection {
        let conn = Connection::open_in_memory().unwrap();
        let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
        let path = format!("{}/db", server_root);
        let database_file_path = format!("{}/{}.db", path, name);
        conn.execute("ATTACH DATABASE ? AS ?", &[database_file_path.as_ref(), name]).unwrap();
        conn
    }
}

const SCHEMA_NAME: &str = "Test";

impl MigrationConnector for SqlMigrationConnector {
    type DatabaseMigrationStep = SqlMigrationStep;

    fn initialize(&self) {
        let conn = Self::new_conn(SCHEMA_NAME);
        let mut m = barrel::Migration::new().schema(SCHEMA_NAME);
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

        dbg!(conn.execute(&sql_str, NO_PARAMS).unwrap());
    }

    fn reset(&self){
        let conn = Self::new_conn(SCHEMA_NAME);
        let sql_str = r#"
            DELETE FROM "Test"."_Migration";
        "#;

        dbg!(conn.execute(&sql_str, NO_PARAMS).unwrap());
    }

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