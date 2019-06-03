mod database_schema_calculator;
mod database_schema_differ;
mod sql_database_migration_steps_inferrer;
mod sql_database_step_applier;
mod sql_destructive_changes_checker;
mod sql_migration_persistence;
mod sql_migration_step;

use barrel;
use barrel::backend::Sqlite;
use barrel::types;
use database_inspector::DatabaseInspector;
use database_inspector::DatabaseInspectorImpl;
use migration_connector::*;
use rusqlite::{Connection, NO_PARAMS};
use serde_json;
use sql_database_migration_steps_inferrer::*;
use sql_database_step_applier::*;
use sql_destructive_changes_checker::*;
use sql_migration_persistence::*;
pub use sql_migration_step::*;
use std::sync::Arc;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector {
    schema_name: String,
    migration_persistence: Arc<MigrationPersistence>,
    sql_database_migration_steps_inferrer: Arc<DatabaseMigrationStepsInferrer<SqlMigration>>,
    database_step_applier: Arc<DatabaseMigrationStepApplier<SqlMigration>>,
    destructive_changes_checker: Arc<DestructiveChangesChecker<SqlMigration>>,
}

impl SqlMigrationConnector {
    // FIXME: this must take the config as a param at some point
    pub fn new(schema_name: String) -> SqlMigrationConnector {
        let migration_persistence = Arc::new(SqlMigrationPersistence::new(Self::new_conn(&schema_name)));
        let sql_database_migration_steps_inferrer = Arc::new(SqlDatabaseMigrationStepsInferrer {
            inspector: Box::new(DatabaseInspectorImpl::new(Self::new_conn(&schema_name))),
            schema_name: schema_name.to_string(),
        });
        let database_step_applier = Arc::new(SqlDatabaseStepApplier::new(
            Self::new_conn(&schema_name),
            schema_name.clone(),
        ));
        let destructive_changes_checker = Arc::new(SqlDestructiveChangesChecker {});
        SqlMigrationConnector {
            schema_name,
            migration_persistence,
            sql_database_migration_steps_inferrer,
            database_step_applier,
            destructive_changes_checker,
        }
    }

    fn new_conn(name: &str) -> Connection {
        let conn = Connection::open_in_memory().unwrap();
        let database_file_path = Self::database_file_path(&name);
        conn.execute("ATTACH DATABASE ? AS ?", &[database_file_path.as_ref(), name])
            .unwrap();
        conn
    }

    fn database_file_path(name: &str) -> String {
        let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
        let path = format!("{}/db", server_root);
        let database_file_path = format!("{}/{}.db", path, name);
        database_file_path
    }
}

impl MigrationConnector for SqlMigrationConnector {
    type DatabaseMigration = SqlMigration;

    fn initialize(&self) {
        let conn = Self::new_conn(&self.schema_name);
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

        dbg!(conn.execute(&sql_str, NO_PARAMS).unwrap());
    }

    fn reset(&self) {
        let conn = Self::new_conn(&self.schema_name);
        let sql_str = format!(r#"DELETE FROM "{}"."_Migration";"#, self.schema_name);

        dbg!(conn.execute(&sql_str, NO_PARAMS).unwrap());
        let _ = std::fs::remove_file(Self::database_file_path(&self.schema_name)); // ignore potential errors
    }

    fn migration_persistence(&self) -> Arc<MigrationPersistence> {
        Arc::clone(&self.migration_persistence)
    }

    fn database_steps_inferrer(&self) -> Arc<DatabaseMigrationStepsInferrer<SqlMigration>> {
        Arc::clone(&self.sql_database_migration_steps_inferrer)
    }

    fn database_step_applier(&self) -> Arc<DatabaseMigrationStepApplier<SqlMigration>> {
        Arc::clone(&self.database_step_applier)
    }

    fn destructive_changes_checker(&self) -> Arc<DestructiveChangesChecker<SqlMigration>> {
        Arc::clone(&self.destructive_changes_checker)
    }

    fn deserialize_database_steps(&self, json: String) -> SqlMigration {
        serde_json::from_str(&json).unwrap()
    }

    fn database_inspector(&self) -> Box<DatabaseInspector> {
        Box::new(DatabaseInspectorImpl::new(SqlMigrationConnector::new_conn(
            &self.schema_name,
        )))
    }
}
