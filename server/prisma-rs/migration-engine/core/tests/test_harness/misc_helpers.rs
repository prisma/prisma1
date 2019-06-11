use database_inspector::*;
use datamodel;
use migration_core::MigrationEngine;
use std::panic;

const SCHEMA_NAME: &str = "migration_engine";

pub fn parse(datamodel_string: &str) -> datamodel::Datamodel {
    match datamodel::parse(datamodel_string) {
        Ok(s) => s,
        Err(errs) => {
            for err in errs.to_iter() {
                err.pretty_print(&mut std::io::stderr().lock(), "", datamodel_string)
                    .unwrap();
            }
            panic!("Schema parsing failed. Please see error above.")
        }
    }
}

pub fn run_test_with_engine<T, X>(test: T) -> X
where
    T: FnOnce(Box<MigrationEngine>) -> X + panic::UnwindSafe,
{
    // SETUP
    let underlying_db_must_exist = true;
    let engine = MigrationEngine::new(&test_config(), underlying_db_must_exist);
    let connector = engine.connector();
    connector.reset();
    engine.init();

    // TEST
    let result = panic::catch_unwind(|| test(engine));
    assert!(result.is_ok());
    result.unwrap()
}

pub fn introspect_database(engine: &Box<MigrationEngine>) -> DatabaseSchema {
    let inspector = engine.connector().database_inspector();
    let mut result = inspector.introspect(&SCHEMA_NAME.to_string());
    // the presence of the _Migration table makes assertions harder. Therefore remove it from the result.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
    result
}

pub fn test_config_json_escaped() -> String {
    let config = test_config();
    serde_json::to_string(&serde_json::Value::String(config)).unwrap()
}

fn test_config() -> String {
    sqlite_test_config()
    // postgres_test_config()
}

fn sqlite_test_config() -> String {
    format!(r#"
        source my_db {{
            type = "sqlite"
            url = "file:{}"
            default = true
        }}
    "#, sqlite_test_file())
}

pub fn sqlite_test_file() -> String {
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let database_folder_path = format!("{}/db", server_root);
    let file_path = format!("{}/{}.db", database_folder_path, SCHEMA_NAME);
    file_path
}

fn postgres_test_config() -> String {
    r#"
        source my_db {
            type = "postgres"
            url = "postgresql://postgres:prisma@127.0.0.1:5432/db"
            default = true
        }
    "#.to_string()
}