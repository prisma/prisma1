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
use prisma_query::connector::{Sqlite, PostgreSql};
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
use postgres::{ Config as PostgresConfig };
use std::time::Duration;
use std::fs;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector {
    folder_path: Option<String>,
    schema_name: String,
    migration_persistence: Arc<MigrationPersistence>,
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
        let parsed_url = Url::parse(url).expect("Parsing of the provided connector url failed.");

        match sql_family { 
            SqlFamily::Sqlite => {
                assert!(url.starts_with("file:"), "the url for sqlite must start with 'file:'");
                let path = Path::new(&url);
                let schema_name = path.file_stem().expect("file url must contain a file name").to_str().unwrap().to_string();
                let folder_path = path.parent().unwrap().to_str().unwrap().to_string();
                let mut stripped_path = folder_path.clone();
                stripped_path.replace_range(..5, "");  // remove the prefix "file:"
                let test_mode = false;
                let conn = Arc::new(Sqlite::new(stripped_path.clone(), 32, test_mode).unwrap());
                Self::create_connector(conn, schema_name, Some(stripped_path))
            },
            SqlFamily::Postgres => {
                let mut config = PostgresConfig::new();
                if let Some(host) = parsed_url.host_str() {
                    config.host(host);
                }
                config.user(parsed_url.username());
                if let Some(password) = parsed_url.password(){
                    config.password(password);
                }
                let mut db_name = parsed_url.path().to_string();
                db_name.replace_range(..1, ""); // strip leading slash
                config.dbname(&db_name);
                config.connect_timeout(Duration::from_secs(5));

                let conn = Arc::new(PostgreSql::new(config, 32).unwrap());
                Self::create_connector(conn, db_name, None)
            },
            _ => unimplemented!()
        }
    }

    fn create_connector<C: Connectional + 'static>(conn: Arc<C>, schema_name: String, folder_path: Option<String>) -> SqlMigrationConnector {
        let migration_persistence = Arc::new(SqlMigrationPersistence {
            connection: Arc::clone(&conn),
            schema_name: schema_name.clone(),
            folder_path: folder_path.clone(),
        });
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
        if let Some(folder_path) = &self.folder_path {
            fs::create_dir_all(folder_path).expect("creating the database folder failed");
        }
        self.migration_persistence.init();
    }

    fn reset(&self) {
        self.migration_persistence.reset();
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

    fn empty_database_migration(&self) -> SqlMigration {
        SqlMigration::empty()
    }

    fn database_inspector(&self) -> Arc<DatabaseInspector> {
        Arc::clone(&self.database_inspector)
    }
}
