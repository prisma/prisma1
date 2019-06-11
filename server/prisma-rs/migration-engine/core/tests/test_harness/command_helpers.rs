

use migration_core::commands::*;
use migration_core::MigrationEngine;
use migration_connector::MigrationStep;
use super::introspect_database;
use database_inspector::*;

const DUMMY_SOURCE_CONFIG: &str = ""; // all commands already run against an initialised engine. Hence the content of this field is not used anymore.

pub fn infer_and_apply(engine: &Box<MigrationEngine>, datamodel: &str) -> DatabaseSchema {
    infer_and_apply_with_migration_id(&engine, &datamodel, "the-migration-id")
}


pub fn infer_and_apply_with_migration_id(
    engine: &Box<MigrationEngine>,
    datamodel: &str,
    migration_id: &str,
) -> DatabaseSchema {

    let input = InferMigrationStepsInput {
        source_config: DUMMY_SOURCE_CONFIG.to_string(),
        migration_id: migration_id.to_string(),
        datamodel: datamodel.to_string(),
        assume_to_be_applied: Vec::new(),
    };
    let steps = run_infer_command(&engine, input);

    apply_migration(&engine, steps, migration_id)
}


pub fn run_infer_command(engine: &Box<MigrationEngine>, input: InferMigrationStepsInput) -> Vec<MigrationStep> {
    let cmd = InferMigrationStepsCommand::new(input);
    let output = cmd.execute(&engine).expect("InferMigration failed");
    assert!(
        output.general_errors.is_empty(),
        format!("InferMigration returned unexpected errors: {:?}", output.general_errors)
    );

    output.datamodel_steps
}


pub fn apply_migration(engine: &Box<MigrationEngine>, steps: Vec<MigrationStep>, migration_id: &str) -> DatabaseSchema {
    let input = ApplyMigrationInput {
        source_config: DUMMY_SOURCE_CONFIG.to_string(),
        migration_id: migration_id.to_string(),
        steps: steps,
        force: None,
    };
    let cmd = ApplyMigrationCommand::new(input);
    let output = cmd.execute(&engine).expect("ApplyMigration failed");
    assert!(
        output.general_errors.is_empty(),
        format!("ApplyMigration returned unexpected errors: {:?}", output.general_errors)
    );

    introspect_database(&engine)
}


pub fn unapply_migration(engine: &Box<MigrationEngine>) -> DatabaseSchema {
    let input = UnapplyMigrationInput {
        source_config: DUMMY_SOURCE_CONFIG.to_string(),
    };
    let cmd = UnapplyMigrationCommand::new(input);
    let _ = cmd.execute(&engine);

    introspect_database(&engine)
}