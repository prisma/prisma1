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
use prisma_query::Connectional;
use serde_json;
use sql_database_migration_inferrer::*;
use sql_database_step_applier::*;
use sql_destructive_changes_checker::*;
pub use sql_migration::*;
use sql_migration_persistence::*;
use std::sync::Arc;
use url::Url;
use std::path::Path;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector {
    folder_path: Option<String>,
    schema_name: String,
    migration_persistence: Arc<MigrationPersistence>,
    sql_migration_persistence: Arc<SqlMigrationPersistence<SqliteDatabaseClient>>,
    database_migration_inferrer: Arc<DatabaseMigrationInferrer<SqlMigration>>,
    database_migration_step_applier: Arc<DatabaseMigrationStepApplier<SqlMigration>>,
    destructive_changes_checker: Arc<DestructiveChangesChecker<SqlMigration>>,
    database_inspector: Arc<DatabaseInspector>,
}

pub enum SqlFamily {
    Sqlite,
    Postgres,
    Mysql
}

impl SqlMigrationConnector {
    pub fn new(sql_family: SqlFamily, url: &str) -> SqlMigrationConnector {
        let _ = Url::parse(url).expect("Parsing of the provided connector url failed.");

        let (conn, folder_path, schema_name) = match sql_family { 
            SqlFamily::Sqlite => {
                let path = Path::new(&url);
                let schema_name = path.file_stem().expect("file url must contain a file name").to_str().unwrap().to_string();
                let folder_path = path.parent().unwrap().to_str().unwrap().to_string();
                let test_mode = false;
                let conn = Arc::new(SqliteDatabaseClient::new(folder_path.clone(), 32, test_mode).unwrap());
                (conn, Some(folder_path), schema_name)
            },
            _ => unimplemented!()
        };

        let migration_persistence = Arc::new(SqlMigrationPersistence {
            connection: Arc::clone(&conn),
            schema_name: schema_name.clone(),
        });
        let sql_migration_persistence = Arc::clone(&migration_persistence);
        let database_migration_inferrer = Arc::new(SqlDatabaseMigrationInferrer {
            inspector: Box::new(DatabaseInspectorImpl {
                connection: Arc::clone(&conn),
            }),
            schema_name: schema_name.to_string(),
        });
        let database_migration_step_applier = Arc::new(SqlDatabaseStepApplier {
            schema_name: schema_name.clone(),
            conn: Arc::clone(&conn),
        });
        let destructive_changes_checker = Arc::new(SqlDestructiveChangesChecker {});
        SqlMigrationConnector {
            folder_path,
            schema_name,
            migration_persistence,
            sql_migration_persistence,
            database_migration_inferrer,
            database_migration_step_applier,
            destructive_changes_checker,
            database_inspector: Arc::new(DatabaseInspectorImpl {
                connection: Arc::clone(&conn),
            }),
        }
    }
}

impl MigrationConnector for SqlMigrationConnector {
    type DatabaseMigration = SqlMigration;

    fn initialize(&self) {
        self.sql_migration_persistence.init();
    }

    fn reset(&self) {
        println!("MigrationConnector.reset()");
        let sql_str = format!(r#"DELETE FROM "{}"."_Migration";"#, self.schema_name);
        let _ = self.sql_migration_persistence.connection.with_connection(&self.schema_name, |conn| conn.query_raw(&sql_str, &[]));

        if let Some(ref folder_path) = self.folder_path {
            let mut file_path = format!("{}/{}.db", folder_path, self.schema_name);
            file_path.replace_range(..5, ""); // remove the prefix "file:"
            println!("FILE PATH {}", file_path);
            let _ = dbg!(std::fs::remove_file(file_path)); // ignore potential errors
        }
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

    fn database_inspector(&self) -> Arc<DatabaseInspector> {
        Arc::clone(&self.database_inspector)
    }
}
