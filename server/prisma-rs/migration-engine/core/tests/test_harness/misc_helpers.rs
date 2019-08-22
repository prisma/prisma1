use datamodel;
use migration_connector::*;
use migration_core::{
    api::{GenericApi, MigrationApi},
    commands::ResetCommand,
    parse_datamodel,
};
use prisma_query::connector::{MysqlParams, PostgresParams};
use sql_migration_connector::{migration_database::*, SqlFamily, SqlMigrationConnector};
use std::convert::TryFrom;
use url::Url;
use database_introspection::{DatabaseSchema, IntrospectionConnection, IntrospectionConnector};
use std::sync::Arc;

pub const SCHEMA_NAME: &str = "migration-engine";

pub fn parse(datamodel_string: &str) -> datamodel::Datamodel {
    parse_datamodel(datamodel_string).unwrap()
}

pub fn test_each_connector<F>(test_fn: F)
where
    F: Fn(SqlFamily, &dyn GenericApi) -> () + std::panic::RefUnwindSafe,
{
    test_each_connector_with_ignores(Vec::new(), test_fn);
}

pub fn test_only_connector<F>(sql_family: SqlFamily, test_fn: F)
where
    F: Fn(SqlFamily, &dyn GenericApi) -> () + std::panic::RefUnwindSafe,
{
    let all = vec![SqlFamily::Postgres, SqlFamily::Mysql, SqlFamily::Sqlite];
    let ignores = all.into_iter().filter(|f| f != &sql_family).collect();

    test_each_connector_with_ignores(ignores, test_fn);
}

pub fn test_each_connector_with_ignores<F>(ignores: Vec<SqlFamily>, test_fn: F)
where
    F: Fn(SqlFamily, &dyn GenericApi) -> () + std::panic::RefUnwindSafe,
{
    // POSTGRES
    if !ignores.contains(&SqlFamily::Postgres) {
        println!("Testing with Postgres now");

        let connector = SqlMigrationConnector::postgres(&postgres_url()).unwrap();
        let api = test_api(connector);

        test_fn(SqlFamily::Postgres, &api);
    } else {
        println!("Ignoring Postgres")
    }

    // MYSQL
    if !ignores.contains(&SqlFamily::Mysql) {
        println!("Testing with MySQL now");

        let connector = SqlMigrationConnector::mysql(&mysql_url()).unwrap();
        let api = test_api(connector);

        println!("ENGINE DONE");

        test_fn(SqlFamily::Mysql, &api);
    } else {
        println!("Ignoring MySQL")
    }

    // SQLite
    if !ignores.contains(&SqlFamily::Sqlite) {
        println!("Testing with SQLite now");

        let connector = SqlMigrationConnector::sqlite(&sqlite_test_file()).unwrap();
        let api = test_api(connector);

        test_fn(SqlFamily::Sqlite, &api);
    } else {
        println!("Ignoring SQLite")
    }
}

pub fn test_api<C, D>(connector: C) -> impl GenericApi
where
    C: MigrationConnector<DatabaseMigration = D>,
    D: DatabaseMigrationMarker + Send + Sync + 'static,
{
    let api = MigrationApi::new(connector).unwrap();

    api.handle_command::<ResetCommand>(&serde_json::Value::Null)
        .expect("Engine reset failed");

    api
}

pub fn introspect_database(api: &dyn GenericApi) -> DatabaseSchema {
    let inspector: Box<dyn IntrospectionConnector> = match api.connector_type() {
        "postgresql" => {
            let db = Arc::new(database_wrapper(SqlFamily::Postgres));
            Box::new(database_introspection::postgres::IntrospectionConnector::new(db))
        },
        "sqlite" => {
            let db = Arc::new(database_wrapper(SqlFamily::Sqlite));
            Box::new(database_introspection::sqlite::IntrospectionConnector::new(db))
        },
        "mysql" => {
            let db = Arc::new(database_wrapper(SqlFamily::Mysql));
            Box::new(database_introspection::mysql::IntrospectionConnector::new(db))
        },
        _ => unimplemented!(),
    };

    let mut result = inspector.introspect(&SCHEMA_NAME.to_string()).expect("Introspection failed");

    // the presence of the _Migration table makes assertions harder. Therefore remove it from the result.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();

    result
}

pub fn database_wrapper(sql_family: SqlFamily) -> MigrationDatabaseWrapper {
    MigrationDatabaseWrapper {
        database: database(sql_family).into(),
    }
}

pub fn database(sql_family: SqlFamily) -> Box<dyn MigrationDatabase + Send + Sync + 'static> {
    match sql_family {
        SqlFamily::Postgres => {
            let url = Url::parse(&postgres_url()).unwrap();
            let params = PostgresParams::try_from(url).unwrap();

            Box::new(PostgreSql::new(params).unwrap())
        }
        SqlFamily::Sqlite => Box::new(Sqlite::new(&sqlite_test_file()).unwrap()),
        SqlFamily::Mysql => {
            let url = Url::parse(&mysql_url()).unwrap();
            let params = MysqlParams::try_from(url).unwrap();

            Box::new(Mysql::new(params).unwrap())
        }
    }
}

pub fn sqlite_test_config() -> String {
    format!(
        r#"
        datasource my_db {{
            provider = "sqlite"
            url = "file:{}"
            default = true
        }}
    "#,
        sqlite_test_file()
    )
}

pub fn sqlite_test_file() -> String {
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let database_folder_path = format!("{}/db", server_root);
    let file_path = format!("{}/{}.db", database_folder_path, SCHEMA_NAME);
    file_path
}

pub fn postgres_test_config() -> String {
    format!(
        r#"
        datasource my_db {{
            provider = "postgresql"
            url = "{}"
            default = true
        }}
    "#,
        postgres_url()
    )
}

pub fn mysql_test_config() -> String {
    format!(
        r#"
        datasource my_db {{
            provider = "mysql"
            url = "{}"
            default = true
        }}
    "#,
        mysql_url()
    )
}

pub fn postgres_url() -> String {
    dbg!(format!(
        "postgresql://postgres:prisma@{}:5432/test-db?schema={}",
        db_host_postgres(),
        SCHEMA_NAME
    ))
}

pub fn mysql_url() -> String {
    dbg!(format!("mysql://root:prisma@{}:3306/{}", db_host_mysql(), SCHEMA_NAME))
}

fn db_host_postgres() -> String {
    match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db-postgres".to_string(),
        Err(_) => "127.0.0.1".to_string(),
    }
}

fn db_host_mysql() -> String {
    match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db-mysql".to_string(),
        Err(_) => "127.0.0.1".to_string(),
    }
}
