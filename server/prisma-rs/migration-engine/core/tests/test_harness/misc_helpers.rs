use database_inspector::*;
use datamodel;
use migration_core::{parse_datamodel, MigrationEngine};
use sql_migration_connector::SqlFamily;

pub const SCHEMA_NAME: &str = "migration_engine";

pub fn parse(datamodel_string: &str) -> datamodel::Datamodel {
    parse_datamodel(datamodel_string).unwrap()
}

pub fn test_each_connector<TestFn>(testFn: TestFn)
where
    TestFn: Fn(&MigrationEngine) -> () + std::panic::RefUnwindSafe,
{
    test_each_connector_with_ignores(Vec::new(), testFn);
}

pub fn test_each_connector_with_ignores<TestFn>(ignores: Vec<SqlFamily>, testFn: TestFn)
where
    TestFn: Fn(&MigrationEngine) -> () + std::panic::RefUnwindSafe,
{
    // SQLite
    if !ignores.contains(&SqlFamily::Sqlite) {
        println!("Testing with SQLite now");
        let engine = test_engine(&sqlite_test_config());
        testFn(&engine);
    } else {
        println!("Ignoring SQLite")
    }
    // POSTGRES
    if !ignores.contains(&SqlFamily::Postgres) {
        println!("Testing with Postgres now");
        let engine = test_engine(&postgres_test_config());
        testFn(&engine);
    } else {
        println!("Ignoring Postgres")
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
        _ => unimplemented!(),
    };
    let mut result = inspector.introspect(&SCHEMA_NAME.to_string());
    // the presence of the _Migration table makes assertions harder. Therefore remove it from the result.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
    result
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
            provider = "postgres"
            url = "{}"
            default = true
        }}
    "#,
        postgres_url()
    )
}

pub fn postgres_url() -> String {
    dbg!(format!(
        "postgresql://postgres:prisma@{}:5432/db?schema={}",
        db_host(),
        SCHEMA_NAME
    ))
}

fn db_host() -> String {
    match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db".to_string(),
        Err(_) => "127.0.0.1".to_string(),
    }
}
