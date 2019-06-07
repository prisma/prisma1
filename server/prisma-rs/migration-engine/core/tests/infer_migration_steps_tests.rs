#![allow(non_snake_case)]
mod test_harness;
use datamodel::dml::*;
use migration_connector::steps::*;
use migration_core::commands::*;
use migration_core::MigrationEngine;
use test_harness::*;

const PROJECT_INFO: &str = "the-project-info";

#[test]
fn assume_to_be_applied_must_work() {
    run_test_with_engine(|engine| {
        let dm0 = r#"
            model Blog {
                id: Int @id
            }
        "#;

        migrate_to_with_migration_id(&engine, &dm0, "mig0000");

        let dm1 = r#"
            model Blog {
                id: Int @id
                field1: String
            }
        "#;
        let input1 = InferMigrationStepsInput {
            project_info: PROJECT_INFO.to_string(),
            migration_id: "mig0001".to_string(),
            assume_to_be_applied: Vec::new(),
            data_model: dm1.to_string(),
        };
        let steps1 = run_infer_command(&engine, input1);
        assert_eq!(steps1, vec![create_field_mock("Blog", "field1", ScalarType::String)]);

        let dm2 = r#"
            model Blog {
                id: Int @id
                field1: String
                field2: String
            }
        "#;
        let input2 = InferMigrationStepsInput {
            project_info: PROJECT_INFO.to_string(),
            migration_id: "mig0002".to_string(),
            assume_to_be_applied: steps1,
            data_model: dm2.to_string(),
        };
        let steps2 = run_infer_command(&engine, input2);
        assert_eq!(steps2, vec![create_field_mock("Blog", "field2", ScalarType::String)]);
    });
}

#[test]
fn special_handling_of_watch_migrations() {
    run_test_with_engine(|engine| {
        let dm = r#"
            model Blog {
                id: Int @id
            }
        "#;

        migrate_to_with_migration_id(&engine, &dm, "mig00");

        let dm = r#"
            model Blog {
                id: Int @id
                field1: String
            }
        "#;
        migrate_to_with_migration_id(&engine, &dm, "watch01");

        let dm = r#"
            model Blog {
                id: Int @id
                field1: String
                field2: String
            }
        "#;
        migrate_to_with_migration_id(&engine, &dm, "watch02");

        let dm = r#"
            model Blog {
                id: Int @id
                field1: String
                field2: String
                field3: Int
            }
        "#;
        let input = InferMigrationStepsInput {
            project_info: PROJECT_INFO.to_string(),
            migration_id: "mig02".to_string(),
            assume_to_be_applied: Vec::new(),
            data_model: dm.to_string(),
        };
        let steps = run_infer_command(&engine, input);
        assert_eq!(
            steps,
            vec![
                create_field_mock("Blog", "field1", ScalarType::String),
                create_field_mock("Blog", "field2", ScalarType::String),
                create_field_mock("Blog", "field3", ScalarType::Int),
            ]
        );
    });
}

fn create_field_mock(model: &str, field: &str, scalar_type: ScalarType) -> MigrationStep {
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

fn run_infer_command(engine: &Box<MigrationEngine>, input: InferMigrationStepsInput) -> Vec<MigrationStep> {
    let cmd = InferMigrationStepsCommand::new(input);
    let output = cmd.execute(&engine).expect("InferMigration failed");
    assert!(
        output.general_errors.is_empty(),
        format!("InferMigration returned unexpected errors: {:?}", output.general_errors)
    );

    output.datamodel_steps
}
