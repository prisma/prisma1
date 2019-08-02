pub mod database_inspector;
mod database_schema_calculator;
mod database_schema_differ;
mod error;
pub mod migration_database;
mod sql_database_migration_inferrer;
mod sql_database_step_applier;
mod sql_destructive_changes_checker;
mod sql_migration;
mod sql_migration_persistence;

use database_inspector::DatabaseInspector;
pub use error::*;
use migration_connector::*;
use migration_database::*;
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
use url::Url;

#[allow(unused, dead_code)]
pub struct SqlMigrationConnector {
    pub file_path: Option<String>,
    pub sql_family: SqlFamily,
    pub schema_name: String,
    pub database: Arc<MigrationDatabase>,
    pub migration_persistence: Arc<MigrationPersistence>,
    pub database_migration_inferrer: Arc<DatabaseMigrationInferrer<SqlMigration>>,
    pub database_migration_step_applier: Arc<DatabaseMigrationStepApplier<SqlMigration>>,
    pub destructive_changes_checker: Arc<DestructiveChangesChecker<SqlMigration>>,
    pub database_inspector: Arc<DatabaseInspector>,
}

#[derive(Copy, Clone, PartialEq)]
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
    pub fn exists(sql_family: SqlFamily, url: &str) -> bool {
        match sql_family {
            SqlFamily::Sqlite => {
                let file_path = url.trim_start_matches("file:").to_string();
                PathBuf::from(file_path).exists()
            }
            SqlFamily::Postgres => {
                let helper = Self::postgres_helper(&url);
                let check_sql = format!(
                    "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '{}';",
                    helper.schema
                );
                let result_set = helper.db_connection.query_raw("", &check_sql, &[]);
                result_set.into_iter().next().is_some()
            }
            SqlFamily::Mysql => {
                let mysql_config = PrismaMysqlConfig::parse(&url);
                // we check whether the db exists by trying to connect to it
                match mysql_config.root_connection() {
                    Ok(connection) => {
                        let check_sql = format!(
                            "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '{}';",
                            mysql_config.db_name()
                        );
                        let result_set = connection.query_raw("", &check_sql, &[]);
                        result_set.into_iter().next().is_some()
                    }
                    Err(_) => false,
                }
            }
        }
    }

    pub fn new(sql_family: SqlFamily, url: &str) -> Arc<MigrationConnector<DatabaseMigration = SqlMigration>> {
        match sql_family {
            SqlFamily::Sqlite => {
                let conn = Sqlite::new(url).unwrap();
                let file_path = conn.file_path.clone();
                let schema_name = "lift".to_string();

                Self::create_connector(Arc::new(conn), sql_family, schema_name, Some(file_path))
            }
            SqlFamily::Postgres => {
                assert!(
                    url.starts_with("postgresql:"),
                    "the url for postgres must start with 'postgresql:'"
                );
                let postgres_helper = Self::postgres_helper(&url);
                Self::create_connector(postgres_helper.db_connection, sql_family, postgres_helper.schema, None)
            }
            SqlFamily::Mysql => {
                assert!(url.starts_with("mysql:"), "the url for mysql must start with 'mysql:'");
                let helper = Self::mysql_helper(&url);
                Self::create_connector(helper.db_connection, sql_family, helper.schema, None)
            }
        }
    }

    pub fn postgres_helper(url: &str) -> DatabaseHelper {
        let postgres_config = PrismaPostgresConfig::parse(&url);

        match postgres_config.db_connection() {
            Ok(db_connection) => DatabaseHelper {
                db_connection: Arc::new(db_connection),
                schema: postgres_config.schema().to_string(),
            },
            Err(prisma_query::error::Error::QueryError(_)) => {
                // assume that the error is because the database does not exist yet
                let root_connection = postgres_config
                    .root_connection()
                    .expect("The user does not have root privelege and can not create a new database");

                let db_sql = format!("CREATE DATABASE \"{}\";", postgres_config.db_name());
                let _ = root_connection.query_raw("", &db_sql, &[]); // ignoring errors as there's no CREATE DATABASE IF NOT EXISTS in Postgres

                let conn = postgres_config
                    .db_connection()
                    .expect("Could not acquire connection to new created database");

                DatabaseHelper {
                    db_connection: Arc::new(conn),
                    schema: postgres_config.schema().to_string(),
                }
            }
            Err(err) => panic!("Encountered unrecoverable error: {:?}", err),
        }
    }

    pub fn mysql_helper(url: &str) -> DatabaseHelper {
        let config = PrismaMysqlConfig::parse(&url);
        // we acquire a root connection here because the db connection blocks insanely long when the db does not exist

        let conn = config
            .root_connection()
            .expect("Could not acquire root connection to MySQL");

        DatabaseHelper {
            db_connection: Arc::new(conn),
            schema: config.db_name().to_string(),
        }
    }

    pub fn virtual_variant(
        sql_family: SqlFamily,
        url: &str,
    ) -> Arc<MigrationConnector<DatabaseMigration = SqlMigration>> {
        println!("Loading a virtual connector!");
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

    fn create_connector(
        conn: Arc<MigrationDatabase>,
        sql_family: SqlFamily,
        schema_name: String,
        file_path: Option<String>,
    ) -> Arc<SqlMigrationConnector> {
        let introspection_connection = Arc::new(MigrationDatabaseWrapper { database: Arc::clone(&conn) });
        let inspector: Arc<DatabaseInspector> = match sql_family {
            SqlFamily::Sqlite => {
                let underlying = database_introspection::sqlite::IntrospectionConnector::new(introspection_connection);
                Arc::new(database_inspector::IntrospectionImpl{inner : Box::new(underlying)})
            },
            SqlFamily::Postgres => {
                let underlying = database_introspection::postgres::IntrospectionConnector::new(introspection_connection);
                Arc::new(database_inspector::IntrospectionImpl{inner : Box::new(underlying)})
            },
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
            sql_family: sql_family,
            schema_name: schema_name.clone(),
            conn: Arc::clone(&conn),
        });
        let destructive_changes_checker = Arc::new(SqlDestructiveChangesChecker {});
        Arc::new(SqlMigrationConnector {
            file_path,
            sql_family,
            schema_name,
            database: Arc::clone(&conn),
            migration_persistence,
            database_migration_inferrer,
            database_migration_step_applier,
            destructive_changes_checker,
            database_inspector: Arc::clone(&inspector),
        })
    }
}

#[derive(Debug)]
struct PrismaPostgresConfig {
    url: Url,
    params: prisma_query::connector::PostgresParams,
}

impl PrismaPostgresConfig {
    fn parse(url: &str) -> Self {
        let parse_error = "Parsing of the provided connector url failed.";
        let url = Url::parse(url).expect(parse_error);
        let params = prisma_query::connector::PostgresParams::try_from(url.clone()).expect(parse_error);

        Self { url, params }
    }

    fn db_name(&self) -> &str {
        self.params.dbname.as_str()
    }

    fn schema(&self) -> &str {
        self.params.schema.as_str()
    }

    fn root_connection(&self) -> prisma_query::Result<PostgreSql> {
        let mut url = self.url.clone();
        url.set_path("postgres");
        let params = prisma_query::connector::PostgresParams::try_from(url)?;

        PostgreSql::new(params)
    }

    fn db_connection(&self) -> prisma_query::Result<PostgreSql> {
        let params = prisma_query::connector::PostgresParams::try_from(self.url.clone())?;
        PostgreSql::new(params)
    }
}

struct PrismaMysqlConfig {
    url: Url,
    params: prisma_query::connector::MysqlParams,
}

impl PrismaMysqlConfig {
    fn parse(url: &str) -> Self {
        let parse_error = "Parsing of the provided connector url failed.";
        let url = Url::parse(url).expect(parse_error);
        let params = prisma_query::connector::MysqlParams::try_from(url.clone()).expect(parse_error);

        Self { url, params }
    }

    fn root_connection(&self) -> prisma_query::Result<Mysql> {
        let mut url = self.url.clone();
        url.set_path("");
        let params = prisma_query::connector::MysqlParams::try_from(url)?;

        Mysql::new(params)
    }

    fn db_name(&self) -> &str {
        self.params.dbname.as_str()
    }
}

pub struct DatabaseHelper {
    pub db_connection: Arc<MigrationDatabase>,
    pub schema: String,
}

impl MigrationConnector for SqlMigrationConnector {
    type DatabaseMigration = SqlMigration;

    fn connector_type(&self) -> &'static str {
        self.sql_family.connector_type_string()
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
                let schema_sql = dbg!(format!("CREATE SCHEMA IF NOT EXISTS \"{}\";", &self.schema_name));
                self.database
                    .query_raw("", &schema_sql, &[])
                    .expect("Creation of Postgres Schema failed");
            }
            SqlFamily::Mysql => {
                let schema_sql = dbg!(format!(
                    "CREATE SCHEMA IF NOT EXISTS `{}` DEFAULT CHARACTER SET latin1;",
                    &self.schema_name
                ));
                self.database
                    .query_raw("", &schema_sql, &[])
                    .expect("Creation of Mysql Schema failed");
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

struct VirtualSqlMigrationConnector {
    sql_family: SqlFamily,
    schema_name: String,
}
impl MigrationConnector for VirtualSqlMigrationConnector {
    type DatabaseMigration = SqlMigration;

    fn connector_type(&self) -> &'static str {
        self.sql_family.connector_type_string()
    }

    fn initialize(&self) -> ConnectorResult<()> {
        Ok(())
    }

    fn reset(&self) -> ConnectorResult<()> {
        Ok(())
    }

    fn migration_persistence(&self) -> Arc<MigrationPersistence> {
        Arc::new(EmptyMigrationPersistence {})
    }

    fn database_migration_inferrer(&self) -> Arc<DatabaseMigrationInferrer<SqlMigration>> {
        Arc::new(VirtualSqlDatabaseMigrationInferrer {
            sql_family: self.sql_family,
            schema_name: self.schema_name.clone(),
        })
    }

    fn database_migration_step_applier(&self) -> Arc<DatabaseMigrationStepApplier<SqlMigration>> {
        Arc::new(VirtualSqlDatabaseStepApplier {
            sql_family: self.sql_family,
            schema_name: self.schema_name.clone(),
        })
    }

    fn destructive_changes_checker(&self) -> Arc<DestructiveChangesChecker<SqlMigration>> {
        Arc::new(EmptyDestructiveChangesChecker::new())
    }

    fn deserialize_database_migration(&self, json: serde_json::Value) -> SqlMigration {
        serde_json::from_value(json).expect("Deserializing the database migration failed.")
    }
}
