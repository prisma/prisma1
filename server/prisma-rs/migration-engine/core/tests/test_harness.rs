use database_inspector::*;
use datamodel;
use migration_core::commands::*;
#[allow(dead_code)]
use migration_core::MigrationEngine;
use std::panic;

#[allow(unused)]
pub fn parse(datamodel_string: &str) -> datamodel::Schema {
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
pub fn run_test_with_engine<T>(test: T) -> ()
where
    T: FnOnce(Box<MigrationEngine>) -> () + panic::UnwindSafe,
{
    // SETUP
    let engine = MigrationEngine::new();
    let connector = engine.connector();
    connector.reset();
    connector.initialize();

    // TEST
    let result = panic::catch_unwind(|| test(engine));
    assert!(result.is_ok())
}

pub fn migrate_to(engine: &Box<MigrationEngine>, datamodel: &str) -> DatabaseSchema {
    let project_info = "the-project-info".to_string();
    let migration_id = "the-migration-id".to_string();

    let input = InferMigrationStepsInput {
        project_info: project_info.clone(),
        migration_id: migration_id.clone(),
        data_model: datamodel.to_string(),
    };
    let cmd = InferMigrationStepsCommand::new(input);
    let output = cmd.execute(&engine);

    let input = ApplyMigrationInput {
        project_info: project_info,
        migration_id: migration_id,
        steps: output.datamodel_steps,
        force: false,
    };
    let cmd = ApplyMigrationCommand::new(input);
    let engine = MigrationEngine::new();
    let _output = cmd.execute(&engine);

    let inspector = engine.connector().database_inspector();
    let mut result = inspector.introspect(&engine.schema_name());
    // the presence of the _Migration table makes assertions harder. Therefore remove it.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
    result
}
