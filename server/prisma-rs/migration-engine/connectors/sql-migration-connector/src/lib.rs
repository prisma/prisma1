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
use postgres::Config as PostgresConfig;
use prisma_query::connector::{PostgreSql, Sqlite};
use prisma_query::Connectional;
use serde_json;
use sql_database_migration_inferrer::*;
use sql_database_step_applier::*;
use sql_destructive_changes_checker::*;
pub use sql_migration::*;
use sql_migration_persistence::*;
use std::convert::TryFrom;
use std::fs;
use std::path::Path;
use std::path::PathBuf;
use std::sync::Arc;
use std::time::Duration;
use url::Url;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector {
    pub file_path: Option<String>,
    pub sql_family: SqlFamily,
    pub schema_name: String,
    pub migration_persistence: Arc<MigrationPersistence>,
    pub database_migration_inferrer: Arc<DatabaseMigrationInferrer<SqlMigration>>,
    pub database_migration_step_applier: Arc<DatabaseMigrationStepApplier<SqlMigration>>,
    pub destructive_changes_checker: Arc<DestructiveChangesChecker<SqlMigration>>,
    pub database_inspector: Arc<DatabaseInspector>,
}

#[derive(Copy, Clone)]
pub enum SqlFamily {
    Sqlite,
    Postgres,
    Mysql,
}

impl SqlMigrationConnector {
    #[allow(unused)]
    pub fn exists(sql_family: SqlFamily, url: &str) -> bool {
        match sql_family {
            SqlFamily::Sqlite => {
                let sqlite = Sqlite::try_from(url).expect("Loading SQLite failed");
                sqlite.does_file_exist()
            }
            _ => unimplemented!(),
        }
    }

    pub fn new(sql_family: SqlFamily, url: &str) -> Arc<MigrationConnector<DatabaseMigration = SqlMigration>> {
        let parsed_url = Url::parse(url).expect("Parsing of the provided connector url failed.");
        let connection_limit = 10;

        match sql_family {
            SqlFamily::Sqlite => {
                assert!(url.starts_with("file:"), "the url for sqlite must start with 'file:'");
                let conn = Arc::new(Sqlite::try_from(url).expect("Loading SQLite failed"));
                let schema_name = "lift".to_string();
                let file_path = url.trim_start_matches("file:").to_string();
                Self::create_connector(conn, sql_family, schema_name, Some(file_path))
            }
            SqlFamily::Postgres => {
                let mut config = PostgresConfig::new();
                if let Some(host) = parsed_url.host_str() {
                    config.host(host);
                }
                config.user(parsed_url.username());
                if let Some(password) = parsed_url.password() {
                    config.password(password);
                }
                let mut db_name = parsed_url.path().to_string();
                db_name.replace_range(..1, ""); // strip leading slash
                config.dbname(&db_name);
                config.connect_timeout(Duration::from_secs(5));

                let conn = Arc::new(PostgreSql::new(config, connection_limit).unwrap());
                Self::create_connector(conn, sql_family, db_name, None)
            }
            _ => unimplemented!(),
        }
    }

    pub fn virtual_variant(
        sql_family: SqlFamily,
        url: &str,
    ) -> Arc<MigrationConnector<DatabaseMigration = SqlMigration>> {
        // TODO: duplicated from above
        let path = Path::new(&url);
        let schema_name = path
            .file_stem()
            .expect("file url must contain a file name")
            .to_str()
            .unwrap()
            .to_string();
        Arc::new(VirtualSqlMigrationConnector {
            sql_family: sql_family,
            schema_name: schema_name,
        })
    }

    fn create_connector<C: Connectional + 'static>(
        conn: Arc<C>,
        sql_family: SqlFamily,
        schema_name: String,
        file_path: Option<String>,
    ) -> Arc<SqlMigrationConnector> {
        let migration_persistence = Arc::new(SqlMigrationPersistence {
            connection: Arc::clone(&conn),
            schema_name: schema_name.clone(),
            file_path: file_path.clone(),
        });
        let database_migration_inferrer = Arc::new(SqlDatabaseMigrationInferrer {
            inspector: Box::new(DatabaseInspectorImpl {
                connection: Arc::clone(&conn),
            }),
            schema_name: schema_name.to_string(),
        });
        let database_migration_step_applier = Arc::new(SqlDatabaseStepApplier {
            sql_family: sql_family,
            schema_name: schema_name.clone(),
            conn: Arc::clone(&conn),
        });
        let destructive_changes_checker = Arc::new(SqlDestructiveChangesChecker {});
        Arc::new(SqlMigrationConnector {
            file_path,
            sql_family,
            schema_name,
            migration_persistence,
            database_migration_inferrer,
            database_migration_step_applier,
            destructive_changes_checker,
            database_inspector: Arc::new(DatabaseInspectorImpl {
                connection: Arc::clone(&conn),
            }),
        })
    }
}

impl MigrationConnector for SqlMigrationConnector {
    type DatabaseMigration = SqlMigration;

    fn initialize(&self) {
        if let Some(file_path) = &self.file_path {
            let path_buf = PathBuf::from(&file_path);
            match path_buf.parent() {
                Some(parent_directory) => {
                    fs::create_dir_all(parent_directory).expect("creating the database folders failed")
                }
                None => {}
            }
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

    fn database_inspector(&self) -> Arc<DatabaseInspector> {
        Arc::clone(&self.database_inspector)
    }
}

struct VirtualSqlMigrationConnector {
    sql_family: SqlFamily,
    schema_name: String,
}
impl MigrationConnector for VirtualSqlMigrationConnector {
    type DatabaseMigration = SqlMigration;

    fn initialize(&self) {}

    fn reset(&self) {}

    fn migration_persistence(&self) -> Arc<MigrationPersistence> {
        Arc::new(EmptyMigrationPersistence {})
    }

    fn database_migration_inferrer(&self) -> Arc<DatabaseMigrationInferrer<SqlMigration>> {
        Arc::new(VirtualSqlDatabaseMigrationInferrer {
            schema_name: self.schema_name.clone(),
        })
    }

    fn database_migration_step_applier(&self) -> Arc<DatabaseMigrationStepApplier<SqlMigration>> {
        Arc::new(VirtualSqlDatabaseStepApplier {
            sql_family: self.sql_family.clone(),
            schema_name: self.schema_name.clone(),
        })
    }

    fn destructive_changes_checker(&self) -> Arc<DestructiveChangesChecker<SqlMigration>> {
        Arc::new(EmptyDestructiveChangesChecker::new())
    }

    fn deserialize_database_migration(&self, json: serde_json::Value) -> SqlMigration {
        serde_json::from_value(json).unwrap()
    }

    fn database_inspector(&self) -> Arc<DatabaseInspector> {
        Arc::new(DatabaseInspector::empty())
    }
}
