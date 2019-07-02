use sql_migration_connector::database_inspector::*;
use sql_migration_connector::SqlMigrationConnector;
use datamodel;
use migration_core::{parse_datamodel, MigrationEngine};
use sql_migration_connector::SqlFamily;
use prisma_query::Connectional;
use std::sync::Arc;
use prisma_query::connector::Sqlite;
use std::convert::TryFrom;

pub const SCHEMA_NAME: &str = "migration_engine";

pub fn parse(datamodel_string: &str) -> datamodel::Datamodel {
    parse_datamodel(datamodel_string).unwrap()
}

pub fn test_each_connector<TestFn>(testFn: TestFn)
where
    TestFn: Fn(SqlFamily, &MigrationEngine) -> () + std::panic::RefUnwindSafe,
{
    test_each_connector_with_ignores(Vec::new(), testFn);
}

pub fn test_only_connector<TestFn>(sql_family: SqlFamily, testFn: TestFn)
where
    TestFn: Fn(SqlFamily, &MigrationEngine) -> () + std::panic::RefUnwindSafe,
{
    let all = vec![SqlFamily::Postgres, SqlFamily::Mysql, SqlFamily::Sqlite];
    let ignores = all.into_iter().filter(|f| f != &sql_family).collect();
    test_each_connector_with_ignores(ignores, testFn);
}

pub fn test_each_connector_with_ignores<TestFn>(ignores: Vec<SqlFamily>, testFn: TestFn)
where
    TestFn: Fn(SqlFamily, &MigrationEngine) -> () + std::panic::RefUnwindSafe,
{
    // SQLite
    if !ignores.contains(&SqlFamily::Sqlite) {
        println!("Testing with SQLite now");
        let engine = test_engine(&sqlite_test_config());
        testFn(SqlFamily::Sqlite, &engine);
    } else {
        println!("Ignoring SQLite")
    }
    // POSTGRES
    if !ignores.contains(&SqlFamily::Postgres) {
        println!("Testing with Postgres now");
        let engine = test_engine(&postgres_test_config());
        testFn(SqlFamily::Postgres, &engine);
    } else {
        println!("Ignoring Postgres")
    }
    // MYSQL
    if !ignores.contains(&SqlFamily::Mysql) {
        println!("Testing with MySQL now");
        let engine = test_engine(&mysql_test_config());
        testFn(SqlFamily::Mysql, &engine);
    } else {
        println!("Ignoring MySQL")
    }
}

pub fn test_engine(config: &str) -> Box<MigrationEngine> {
    let underlying_db_must_exist = true;
    let engine = MigrationEngine::new(config, underlying_db_must_exist);
    engine.reset().expect("Engine reset failed.");
    engine.init().expect("Engine init failed");
    engine
}

pub fn introspect_database(engine: &MigrationEngine) -> DatabaseSchema {
    let inspector: Box<DatabaseInspector> = match engine.connector().connector_type() {
        "postgres" => Box::new(DatabaseInspector::postgres(postgres_url())),
        "sqlite" => Box::new(DatabaseInspector::sqlite(sqlite_test_file())),
        "mysql" => Box::new(DatabaseInspector::mysql(mysql_url())),
        _ => unimplemented!(),
    };
    let mut result = inspector.introspect(&SCHEMA_NAME.to_string());
    // the presence of the _Migration table makes assertions harder. Therefore remove it from the result.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
    result
}

pub fn connectional(sql_family: SqlFamily) -> Arc<Connectional> {
    match sql_family {
        SqlFamily::Postgres => postgres_connectional(),
        SqlFamily::Sqlite => sqlite_connectional(),
        SqlFamily::Mysql => mysql_connectional(),
    }
}

fn postgres_connectional() -> Arc<Connectional> {
    let postgres = SqlMigrationConnector::postgres_helper(&postgres_url());
    postgres.db_connection
}

fn sqlite_connectional() -> Arc<Connectional> {
    let url = format!("file:{}", sqlite_test_file());
    Arc::new(Sqlite::try_from(url.as_ref()).expect("Loading SQLite failed"))
}

fn mysql_connectional() -> Arc<Connectional> {
    let helper = SqlMigrationConnector::mysql_helper(&mysql_url());
    helper.db_connection
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
        "postgresql://postgres:prisma@{}:5432/db?schema={}",
        db_host_postgres(),
        SCHEMA_NAME
    ))
}

pub fn mysql_url() -> String {
    dbg!(format!(
        "mysql://root:prisma@{}:3306/{}",
        db_host_mysql(),
        SCHEMA_NAME
    ))
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
