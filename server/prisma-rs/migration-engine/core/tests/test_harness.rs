use database_inspector::*;
use datamodel;
use migration_connector::MigrationStep;
use migration_core::commands::*;
#[allow(dead_code)]
use migration_core::MigrationEngine;
use std::panic;

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
    let engine = MigrationEngine::new();
    let connector = engine.connector();
    connector.reset();
    engine.init();

    // TEST
    let result = panic::catch_unwind(|| test(engine));
    assert!(result.is_ok());
    result.unwrap()
}

#[allow(unused)]
pub fn infer_and_apply(engine: &Box<MigrationEngine>, datamodel: &str) -> DatabaseSchema {
    infer_and_apply_with_migration_id(&engine, &datamodel, "the-migration-id")
}

#[allow(unused)]
pub fn infer_and_apply_with_migration_id(
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
    let steps = run_infer_command(&engine, input);

    let input = ApplyMigrationInput {
        project_info: project_info,
        migration_id: migration_id.to_string(),
        steps: steps,
        force: None,
        dry_run: None,
    };
    let cmd = ApplyMigrationCommand::new(input);
    let engine = MigrationEngine::new();
    let _output = cmd.execute(&engine).expect("applyMigration failed");

    introspect_database(&engine)
}

#[allow(unused)]
pub fn run_infer_command(engine: &Box<MigrationEngine>, input: InferMigrationStepsInput) -> Vec<MigrationStep> {
    let cmd = InferMigrationStepsCommand::new(input);
    let output = cmd.execute(&engine).expect("InferMigration failed");
    assert!(
        output.general_errors.is_empty(),
        format!("InferMigration returned unexpected errors: {:?}", output.general_errors)
    );

    output.datamodel_steps
}

pub fn introspect_database(engine: &Box<MigrationEngine>) -> DatabaseSchema {
    let inspector = engine.connector().database_inspector();
    let mut result = inspector.introspect(&engine.schema_name());
    // the presence of the _Migration table makes assertions harder. Therefore remove it.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
    result
}
