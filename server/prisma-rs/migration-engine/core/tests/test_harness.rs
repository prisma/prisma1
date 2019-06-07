use database_inspector::*;
use datamodel;
use migration_connector::MigrationStep;
use migration_connector::steps::*;
use migration_core::commands::*;
use datamodel::dml::*;
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

    apply_migration(&engine, steps, migration_id)
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

#[allow(unused)]
pub fn apply_migration(engine: &Box<MigrationEngine>, steps: Vec<MigrationStep>, migration_id: &str) -> DatabaseSchema {
    let project_info = "the-project-info".to_string();
    let input = ApplyMigrationInput {
        project_info: project_info,
        migration_id: migration_id.to_string(),
        steps: steps,
        force: None,
        dry_run: None,
    };
    let cmd = ApplyMigrationCommand::new(input);
    let output = cmd.execute(&engine).expect("ApplyMigration failed");
    assert!(
        output.general_errors.is_empty(),
        format!("ApplyMigration returned unexpected errors: {:?}", output.general_errors)
    );

    introspect_database(&engine)
}

#[allow(unused)]
pub fn unapply_migration(engine: &Box<MigrationEngine>) -> DatabaseSchema {
    let project_info = "the-project-info".to_string();

    let input = UnapplyMigrationInput {
        project_info: project_info.clone(),
    };
    let cmd = UnapplyMigrationCommand::new(input);
    let _ = cmd.execute(&engine);

    introspect_database(&engine)
}

pub fn introspect_database(engine: &Box<MigrationEngine>) -> DatabaseSchema {
    let inspector = engine.connector().database_inspector();
    let mut result = inspector.introspect(&engine.schema_name());
    // the presence of the _Migration table makes assertions harder. Therefore remove it.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
    result
}

#[allow(unused)]
pub fn create_field_step(model: &str, field: &str, scalar_type: ScalarType) -> MigrationStep {
    MigrationStep::CreateField(CreateField {
        model: model.to_string(),
        name: field.to_string(),
        tpe: FieldType::Base(scalar_type),
        arity: FieldArity::Required,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: false,
        id: None,
        default: None,
        scalar_list: None,
    })
}

#[allow(unused)]
pub fn create_id_field_step(model: &str, field: &str, scalar_type: ScalarType) -> MigrationStep {
    MigrationStep::CreateField(CreateField {
        model: model.to_string(),
        name: field.to_string(),
        tpe: FieldType::Base(scalar_type),
        arity: FieldArity::Required,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: false,
        id: Some(IdInfo {
                    strategy: IdStrategy::Auto,
                    sequence: None,
                }),
        default: None,
        scalar_list: None,
    })
}

#[allow(unused)]
pub fn create_model_step(model: &str) -> MigrationStep {
    MigrationStep::CreateModel(CreateModel {
        name: model.to_string(),
        db_name: None,
        embedded: false,
    })
}