#[macro_use]
extern crate log;

pub mod database_inspector;
pub mod migration_database;

mod database_schema_calculator;
mod database_schema_differ;
mod error;
mod sql_database_migration_inferrer;
mod sql_database_step_applier;
mod sql_destructive_changes_checker;
mod sql_migration;
mod sql_migration_persistence;

pub use error::*;
pub use sql_migration::*;

use database_inspector::DatabaseInspector;
use migration_connector::*;
use migration_database::*;
use prisma_query::connector::{MysqlParams, PostgresParams};
use serde_json;
use sql_database_migration_inferrer::*;
use sql_database_step_applier::*;
use sql_destructive_changes_checker::*;
use sql_migration_persistence::*;
use std::{convert::TryFrom, fs, path::PathBuf, sync::Arc};
use url::Url;

pub type Result<T> = std::result::Result<T, SqlError>;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector {
    pub url: String,
    pub file_path: Option<String>,
    pub sql_family: SqlFamily,
    pub schema_name: String,
    pub database: Arc<dyn MigrationDatabase + Send + Sync + 'static>,
    pub migration_persistence: Arc<dyn MigrationPersistence>,
    pub database_migration_inferrer: Arc<dyn DatabaseMigrationInferrer<SqlMigration>>,
    pub database_migration_step_applier: Arc<dyn DatabaseMigrationStepApplier<SqlMigration>>,
    pub destructive_changes_checker: Arc<dyn DestructiveChangesChecker<SqlMigration>>,
    pub database_inspector: Arc<dyn DatabaseInspector + Send + Sync + 'static>,
}

#[derive(Debug, Copy, Clone, PartialEq)]
pub enum SqlFamily {
    Sqlite,
    Postgres,
    Mysql,
}

impl SqlFamily {
    fn connector_type_string(&self) -> &'static str {
        match self {
            SqlFamily::Postgres => "postgresql",
            SqlFamily::Mysql => "mysql",
            SqlFamily::Sqlite => "sqlite",
        }
    }
}

impl SqlMigrationConnector {
    pub fn postgres(url: &str) -> crate::Result<Self> {
        let url = Url::parse(url)?;

        let params = PostgresParams::try_from(url.clone())?;

        let dbname = params.dbname.clone();
        let schema = params.schema.clone();

        match PostgreSql::new(params) {
            Ok(conn) => Ok(Self::create_connector(
                url,
                Arc::new(conn),
                SqlFamily::Postgres,
                schema,
                None,
            )),
            Err(prisma_query::error::Error::ConnectionError(_)) => {
                let _ = {
                    let mut url = url.clone();
                    url.set_path("postgres");

                    let params = PostgresParams::try_from(url)?;
                    let connection = PostgreSql::new(params)?;

                    let db_sql = format!("CREATE DATABASE \"{}\";", dbname);

                    connection.query_raw("", &db_sql, &[]) // ignoring errors as there's no CREATE DATABASE IF NOT EXISTS in Postgres
                };

                let params = PostgresParams::try_from(url)?;
                let schema = params.schema.clone();
                let conn = PostgreSql::new(params)?;

                Ok(Self::create_connector(
                    url,
                    Arc::new(conn),
                    SqlFamily::Postgres,
                    schema,
                    None,
                ))
            }
            Err(err) => Err(err.into()),
        }
    }

    pub fn mysql(url: &str) -> crate::Result<Self> {
        let mut url = Url::parse(url)?;

        let schema = {
            let params = MysqlParams::try_from(url.clone())?;
            params.dbname.clone()
        };

        url.set_path("");

        let params = MysqlParams::try_from(url)?;
        let conn = Mysql::new(params)?;

        Ok(Self::create_connector(
            url,
            Arc::new(conn),
            SqlFamily::Mysql,
            schema,
            None,
        ))
    }

    pub fn sqlite(url: &str) -> crate::Result<Self> {
        let conn = Sqlite::new(url)?;
        let file_path = conn.file_path.clone();
        let schema = String::from("lift");

        Ok(Self::create_connector(
            url,
            Arc::new(conn),
            SqlFamily::Sqlite,
            schema,
            Some(file_path),
        ))
    }

    fn create_connector(
        url: &str,
        conn: Arc<dyn MigrationDatabase + Send + Sync + 'static>,
        sql_family: SqlFamily,
        schema_name: String,
        file_path: Option<String>,
    ) -> Self {
        let inspector: Arc<dyn DatabaseInspector + Send + Sync + 'static> = match sql_family {
            SqlFamily::Sqlite => Arc::new(DatabaseInspector::sqlite_with_database(Arc::clone(&conn))),
            SqlFamily::Postgres => Arc::new(DatabaseInspector::postgres_with_database(Arc::clone(&conn))),
            SqlFamily::Mysql => Arc::new(DatabaseInspector::mysql_with_database(Arc::clone(&conn))),
        };

        let migration_persistence = Arc::new(SqlMigrationPersistence {
            sql_family,
            connection: Arc::clone(&conn),
            schema_name: schema_name.clone(),
            file_path: file_path.clone(),
        });

        let database_migration_inferrer = Arc::new(SqlDatabaseMigrationInferrer {
            sql_family,
            inspector: Arc::clone(&inspector),
            schema_name: schema_name.to_string(),
        });

        let database_migration_step_applier = Arc::new(SqlDatabaseStepApplier {
            sql_family,
            schema_name: schema_name.clone(),
            conn: Arc::clone(&conn),
        });

        let destructive_changes_checker = Arc::new(SqlDestructiveChangesChecker {});

        Self {
            url: url.to_string(),
            file_path,
            sql_family,
            schema_name,
            database: Arc::clone(&conn),
            migration_persistence,
            database_migration_inferrer,
            database_migration_step_applier,
            destructive_changes_checker,
            database_inspector: Arc::clone(&inspector),
        }
    }
}

impl MigrationConnector for SqlMigrationConnector {
    type DatabaseMigration = SqlMigration;

    fn connector_type(&self) -> &'static str {
        self.sql_family.connector_type_string()
    }

    fn can_connect(&self) -> bool {
        match self.sql_family {
            SqlFamily::Postgres | SqlFamily::Mysql => self
                .database
                .query_raw("", "SELECT * FROM information_schema.tables;", &[])
                .map(|_| true)
                .unwrap_or(false),
            SqlFamily::Sqlite => unreachable!(),
        }
    }

    fn can_create_database(&self) -> bool {
        match self.sql_family {
            SqlFamily::Postgres => self
                .database
                .query_raw("", "select rolcreatedb from pg_authid where rolname = 'postgres';", &[])
                .map(|_| true)
                .unwrap_or(false),
            SqlFamily::Sqlite | SqlFamily::Mysql => unreachable!(),
        }
    }

    fn create_database(&self) {
        unimplemented!()
    }

    fn initialize(&self) -> ConnectorResult<()> {
        // TODO: this code probably does not ever do anything. The schema/db creation happens already in the helper functions above.
        match self.sql_family {
            SqlFamily::Sqlite => {
                if let Some(file_path) = &self.file_path {
                    let path_buf = PathBuf::from(&file_path);
                    match path_buf.parent() {
                        Some(parent_directory) => {
                            fs::create_dir_all(parent_directory).expect("creating the database folders failed")
                        }
                        None => {}
                    }
                }
            }
            SqlFamily::Postgres => {
                let schema_sql = format!("CREATE SCHEMA IF NOT EXISTS \"{}\";", &self.schema_name);

                debug!("{}", schema_sql);

                self.database.query_raw("", &schema_sql, &[])?;
            }
            SqlFamily::Mysql => {
                let schema_sql = format!(
                    "CREATE SCHEMA IF NOT EXISTS `{}` DEFAULT CHARACTER SET latin1;",
                    &self.schema_name
                );

                debug!("{}", schema_sql);

                self.database.query_raw("", &schema_sql, &[])?;
            }
        }

        self.migration_persistence.init();

        Ok(())
    }

    fn reset(&self) -> ConnectorResult<()> {
        self.migration_persistence.reset();
        Ok(())
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
        serde_json::from_value(json).expect("Deserializing the database migration failed.")
    }
}
