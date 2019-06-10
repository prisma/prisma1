use database_inspector::*;
use datamodel;
use migration_core::commands::*;
#[allow(dead_code)]
use migration_core::MigrationEngine;
use std::panic;

const SCHEMA_NAME: &str = "migration_engine";

#[allow(unused)]
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

#[allow(unused)]
pub fn run_test_with_engine<T, X>(test: T) -> X
where
    T: FnOnce(Box<MigrationEngine>) -> X + panic::UnwindSafe,
{
    // SETUP
    let engine = MigrationEngine::new(&test_config());
    let connector = engine.connector();
    connector.reset();
    engine.init();

    // TEST
    let result = panic::catch_unwind(|| test(engine));
    assert!(result.is_ok());
    result.unwrap()
}

#[allow(unused)]
pub fn migrate_to(engine: &Box<MigrationEngine>, datamodel: &str) -> DatabaseSchema {
    migrate_to_with_migration_id(&engine, &datamodel, "the-migration-id")
}

#[allow(unused)]
pub fn migrate_to_with_migration_id(
    engine: &Box<MigrationEngine>,
    datamodel: &str,
    migration_id: &str,
) -> DatabaseSchema {
    let project_info = "the-project-info".to_string();

    let input = InferMigrationStepsInput {
        project_info: project_info.clone(),
        migration_id: migration_id.to_string(),
        data_model: datamodel.to_string(),
        assume_to_be_applied: Vec::new(),
    };
    let cmd = InferMigrationStepsCommand::new(input);
    let output = cmd.execute(&engine).expect("inferMigrationSteps failed");

    let input = ApplyMigrationInput {
        project_info: project_info,
        migration_id: migration_id.to_string(),
        steps: output.datamodel_steps,
        force: None,
        dry_run: None,
    };
    let cmd = ApplyMigrationCommand::new(input);
    let engine = MigrationEngine::new(&test_config());
    let _output = cmd.execute(&engine).expect("applyMigration failed");

    introspect_database(&engine)
}

pub fn introspect_database(engine: &Box<MigrationEngine>) -> DatabaseSchema {
    let inspector = engine.connector().database_inspector();
    let mut result = inspector.introspect(&SCHEMA_NAME.to_string());
    // the presence of the _Migration table makes assertions harder. Therefore remove it.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
    result
}

#[allow(unused)]
pub fn test_config_json_escaped() -> String {
    let config = test_config();
    serde_json::to_string(&serde_json::Value::String(config)).unwrap()
}

fn test_config() -> String {
    sqlite_test_config()
    // postgres_test_config()
}

fn sqlite_test_config() -> String {
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let database_folder_path = format!("{}/db", server_root);
    let file_path = format!("{}/{}.db", database_folder_path,"migration_engine");
    format!(r#"
        source my_db {{
            type = "sqlite"
            url = "file:{}"
            default = true
        }}
    "#, file_path)
}

#[allow(unused)]
fn postgres_test_config() -> String {
    r#"
        source my_db {
            type = "postgres"
            url = "postgresql://postgres:prisma@127.0.0.1:5432/db"
            default = true
        }
    "#.to_string()
}