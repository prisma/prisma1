pub mod database_inspector;
mod database_schema_calculator;
mod database_schema_differ;
mod error;
mod sql_database_migration_inferrer;
mod sql_database_step_applier;
mod sql_destructive_changes_checker;
mod sql_migration;
mod sql_migration_persistence;

use database_inspector::DatabaseInspector;
pub use error::*;
use migration_connector::*;
use postgres::Config as PostgresConfig;
use prisma_query::connector::{Mysql, PostgreSql, Sqlite};
use prisma_query::Connectional;
use serde_json;
use sql_database_migration_inferrer::*;
use sql_database_step_applier::*;
use sql_destructive_changes_checker::*;
pub use sql_migration::*;
use sql_migration_persistence::*;
use std::borrow::Cow;
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
    pub connectional: Arc<Connectional>,
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
                let sqlite = Sqlite::try_from(url).expect("Loading SQLite failed");
                sqlite.does_file_exist()
            }
            SqlFamily::Postgres => {
                let helper = Self::postgres_helper(&url);
                let check_sql = format!(
                    "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '{}';",
                    helper.schema
                );
                let result_set = helper.db_connection.query_on_raw_connection("", &check_sql, &[]);
                result_set.into_iter().next().is_some()
            }
            SqlFamily::Mysql => {
                let mysql_config = PrismaMysqlConfig::parse(&url);
                // we check whether the db exists by trying to connect to it
                match mysql_config.root_connection() {
                    Ok(connection) => {
                        let check_sql = format!(
                            "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '{}';",
                            mysql_config.db_name
                        );
                        let result_set = connection.query_on_raw_connection("", &check_sql, &[]);
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
                assert!(url.starts_with("file:"), "the url for sqlite must start with 'file:'");
                let conn = Arc::new(Sqlite::try_from(url).expect("Loading SQLite failed"));
                let schema_name = "lift".to_string();
                let file_path = url.trim_start_matches("file:").to_string();
                Self::create_connector(conn, sql_family, schema_name, Some(file_path))
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
                schema: postgres_config.schema,
            },
            Err(prisma_query::error::Error::ConnectionError(_)) => {
                // assume that the error is because the database does not exist yet
                let root_connection = postgres_config
                    .root_connection()
                    .expect("The user does not have root privelege and can not create a new database");

                let db_sql = format!("CREATE DATABASE \"{}\";", &postgres_config.db_name);
                let _ = root_connection.query_on_raw_connection("", &db_sql, &[]); // ignoring errors as there's no CREATE DATABASE IF NOT EXISTS in Postgres

                DatabaseHelper {
                    db_connection: Arc::new(
                        postgres_config
                            .db_connection()
                            .expect("Could not acquire connection to new created database"),
                    ),
                    schema: postgres_config.schema,
                }
            }
            Err(err) => panic!("Encountered unrecoverable error: {:?}", err),
        }
    }

    pub fn mysql_helper(url: &str) -> DatabaseHelper {
        let config = PrismaMysqlConfig::parse(&url);
        // we acquire a root connection here because the db connection blocks insanely long when the db does not exist
        DatabaseHelper {
            db_connection: Arc::new(
                config
                    .root_connection()
                    .expect("Could not acquire root connection to MySQL"),
            ),
            schema: config.db_name,
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
        conn: Arc<Connectional>,
        sql_family: SqlFamily,
        schema_name: String,
        file_path: Option<String>,
    ) -> Arc<SqlMigrationConnector> {
        let inspector: Arc<DatabaseInspector> = match sql_family {
            SqlFamily::Sqlite => Arc::new(DatabaseInspector::sqlite_with_connectional(Arc::clone(&conn))),
            SqlFamily::Postgres => Arc::new(DatabaseInspector::postgres_with_connectional(Arc::clone(&conn))),
            SqlFamily::Mysql => Arc::new(DatabaseInspector::mysql_with_connectional(Arc::clone(&conn))),
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
            connectional: Arc::clone(&conn),
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
    pub host: String,
    pub port: u16,
    pub user_name: String,
    pub password: String,
    pub db_name: String,
    pub schema: String,
    pub connection_limit: u32,
}

impl PrismaPostgresConfig {
    fn parse(url: &str) -> PrismaPostgresConfig {
        let parsed_url = Url::parse(url).expect("Parsing of the provided connector url failed.");
        let host = parsed_url.host_str().unwrap_or("localhost").to_string();
        let port = parsed_url.port().unwrap_or(5432);
        let user_name = parsed_url.username().to_string();
        let password = parsed_url.password().unwrap_or("").to_string();
        let mut db_name = parsed_url.path().to_string(); // strip leading slash
        db_name.replace_range(..1, "");

        let schema = parsed_url
            .query_pairs()
            .into_iter()
            .find(|qp| qp.0 == Cow::Borrowed("schema"))
            .map(|pair| pair.1.to_string())
            .unwrap_or("public".to_string());

        let connection_limit = parsed_url
            .query_pairs()
            .into_iter()
            .find(|qp| qp.0 == Cow::Borrowed("connection_limit"))
            .map(|pair| {
                let as_int: u32 = pair.1.parse().expect("connection_limit parameter was not an int");
                as_int
            })
            .unwrap_or(1);

        PrismaPostgresConfig {
            host,
            port,
            user_name,
            password,
            db_name,
            schema,
            connection_limit,
        }
    }

    fn root_connection(&self) -> prisma_query::Result<PostgreSql> {
        let config = self.config("postgres");
        PostgreSql::new(config, 1)
    }

    fn db_connection(&self) -> prisma_query::Result<PostgreSql> {
        let config = self.config(&self.db_name);
        PostgreSql::new(config, 1)
    }

    fn config(&self, db_name: &str) -> PostgresConfig {
        let mut config = PostgresConfig::new();
        config.host(&self.host);
        config.port(self.port);
        config.user(&self.user_name);
        config.password(&self.password);
        config.dbname(db_name);
        config.connect_timeout(Duration::from_secs(5));
        config
    }
}

struct PrismaMysqlConfig {
    pub host: String,
    pub port: u16,
    pub user_name: String,
    pub password: String,
    pub db_name: String,
    pub connection_limit: u32,
}

impl PrismaMysqlConfig {
    fn parse(url: &str) -> PrismaMysqlConfig {
        let parsed_url = Url::parse(url).expect("the provided URL was invalid");

        let host = parsed_url.host_str().unwrap_or("localhost").to_string();;
        let port = parsed_url.port().unwrap_or(3306);
        let user_name = parsed_url.username().to_string();
        let password = parsed_url.password().unwrap_or("").to_string();
        let db_name = parsed_url
            .path_segments()
            .and_then(|mut segments| segments.next())
            .expect("db name must be set")
            .to_string();

        let connection_limit = parsed_url
            .query_pairs()
            .into_iter()
            .find(|qp| qp.0 == Cow::Borrowed("connection_limit"))
            .map(|pair| {
                let as_int: u32 = pair.1.parse().expect("connection_limit parameter was not an int");
                as_int
            })
            .unwrap_or(1);

        PrismaMysqlConfig {
            host,
            port,
            user_name,
            password,
            db_name,
            connection_limit,
        }
    }

    fn root_connection(&self) -> prisma_query::Result<Mysql> {
        let config = self.config();
        Mysql::new(config)
    }

    #[allow(unused)]
    fn db_connection(&self) -> prisma_query::Result<Mysql> {
        let mut config = self.config();
        config.db_name(Some(self.db_name.to_string()));
        Mysql::new(config)
    }

    fn config(&self) -> mysql::OptsBuilder {
        let mut builder = mysql::OptsBuilder::new();

        builder.ip_or_hostname(Some(self.host.to_string()));
        builder.tcp_port(self.port);
        builder.user(Some(self.user_name.clone()));
        builder.pass(Some(self.password.clone()));
        builder.verify_peer(false);
        builder.stmt_cache_size(Some(1000));
        builder.tcp_connect_timeout(Some(std::time::Duration::from_millis(5000)));

        builder
    }
}

pub struct DatabaseHelper {
    pub db_connection: Arc<Connectional>,
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
                self.connectional
                    .query_on_raw_connection(&self.schema_name, &schema_sql, &[])
                    .expect("Creation of Postgres Schema failed");
            }
            SqlFamily::Mysql => {
                let schema_sql = dbg!(format!(
                    "CREATE SCHEMA IF NOT EXISTS `{}` DEFAULT CHARACTER SET latin1;",
                    &self.schema_name
                ));
                self.connectional
                    .query_on_raw_connection(&self.schema_name, &schema_sql, &[])
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
