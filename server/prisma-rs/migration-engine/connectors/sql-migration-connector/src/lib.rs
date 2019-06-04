mod database_schema_calculator;
mod database_schema_differ;
mod sql_database_migration_inferrer;
mod sql_database_step_applier;
mod sql_destructive_changes_checker;
mod sql_migration;
mod sql_migration_persistence;

use database_inspector::DatabaseInspector;
use database_inspector::DatabaseInspectorImpl;
use migration_connector::*;
use prisma_query::connector::Sqlite as SqliteDatabaseClient;
use rusqlite::{Connection, NO_PARAMS};
use serde_json;
use sql_database_migration_inferrer::*;
use sql_database_step_applier::*;
use sql_destructive_changes_checker::*;
pub use sql_migration::*;
use sql_migration_persistence::*;
use std::sync::Arc;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector {
    schema_name: String,
    migration_persistence: Arc<MigrationPersistence>,
    sql_migration_persistence: Arc<SqlMigrationPersistence<SqliteDatabaseClient>>,
    database_migration_inferrer: Arc<DatabaseMigrationInferrer<SqlMigration>>,
    database_migration_step_applier: Arc<DatabaseMigrationStepApplier<SqlMigration>>,
    destructive_changes_checker: Arc<DestructiveChangesChecker<SqlMigration>>,
}

impl SqlMigrationConnector {
    // FIXME: this must take the config as a param at some point
    pub fn new(schema_name: String) -> SqlMigrationConnector {
        let test_mode = false;
        let conn =
            std::sync::Arc::new(SqliteDatabaseClient::new(Self::databases_folder_path(), 32, test_mode).unwrap());

        let migration_persistence = Arc::new(SqlMigrationPersistence {
            connection: Arc::clone(&conn),
            schema_name: schema_name.clone(),
        });
        let sql_migration_persistence = Arc::clone(&migration_persistence);
        let database_migration_inferrer = Arc::new(SqlDatabaseMigrationInferrer {
            inspector: Box::new(DatabaseInspectorImpl::new(Self::new_conn(&schema_name))),
            schema_name: schema_name.to_string(),
        });
        let database_migration_step_applier = Arc::new(SqlDatabaseStepApplier {
            schema_name: schema_name.clone(),
            conn: Arc::clone(&conn),
        });
        let destructive_changes_checker = Arc::new(SqlDestructiveChangesChecker {});
        SqlMigrationConnector {
            schema_name,
            migration_persistence,
            sql_migration_persistence,
            database_migration_inferrer,
            database_migration_step_applier,
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
        let path = Self::databases_folder_path();
        let database_file_path = format!("{}/{}.db", path, name);
        database_file_path
    }

    fn databases_folder_path() -> String {
        let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
        format!("{}/db", server_root)
    }
}

impl MigrationConnector for SqlMigrationConnector {
    type DatabaseMigration = SqlMigration;

    fn initialize(&self) {
        self.sql_migration_persistence.init();
    }

    fn reset(&self) {
        println!("MigrationConnector.reset()");
        let conn = Self::new_conn(&self.schema_name);
        let sql_str = format!(r#"DELETE FROM "{}"."_Migration";"#, self.schema_name);

        let _ = conn.execute(&sql_str, NO_PARAMS);
        let _ = std::fs::remove_file(Self::database_file_path(&self.schema_name)); // ignore potential errors
    }

    fn migration_persistence(&self) -> Arc<MigrationPersistence> {
        Arc::clone(&self.migration_persistence)
    }

    fn database_migration_inferrer(&self) -> Arc<DatabaseMigrationInferrer<SqlMigration>> {
        Arc::clone(&self.database_migration_inferrer)
    }

    fn database_migration_step_applier(&self) -> Arc<DatabaseMigrationStepApplier<SqlMigration>> {
        Arc::clone(&self.database_migration_step_applier)
    }

    fn destructive_changes_checker(&self) -> Arc<DestructiveChangesChecker<SqlMigration>> {
        Arc::clone(&self.destructive_changes_checker)
    }

    fn deserialize_database_migration(&self, json: serde_json::Value) -> SqlMigration {
        serde_json::from_value(json).unwrap()
    }

    fn database_inspector(&self) -> Box<DatabaseInspector> {
        Box::new(DatabaseInspectorImpl::new(SqlMigrationConnector::new_conn(
            &self.schema_name,
        )))
    }
}
