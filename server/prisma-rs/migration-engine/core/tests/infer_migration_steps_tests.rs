#![allow(non_snake_case)]
#![allow(unused)]
mod test_harness;
use datamodel::dml::*;
use migration_core::commands::*;
use pretty_assertions::{assert_eq, assert_ne};
use test_harness::*;

#[test]
fn assume_to_be_applied_must_work() {
    test_each_connector(|_, api| {
        let dm0 = r#"
            model Blog {
                id Int @id
            }
        "#;

        infer_and_apply_with_migration_id(api, &dm0, "mig0000");

        let dm1 = r#"
            model Blog {
                id Int @id
                field1 String
            }
        "#;
        let input1 = InferMigrationStepsInput {
            migration_id: "mig0001".to_string(),
            assume_to_be_applied: Vec::new(),
            datamodel: dm1.to_string(),
        };
        let steps1 = run_infer_command(api, input1);
        assert_eq!(steps1, vec![create_field_step("Blog", "field1", ScalarType::String)]);

        let dm2 = r#"
            model Blog {
                id Int @id
                field1 String
                field2 String
            }
        "#;
        let input2 = InferMigrationStepsInput {
            migration_id: "mig0002".to_string(),
            assume_to_be_applied: steps1,
            datamodel: dm2.to_string(),
        };
        let steps2 = run_infer_command(api, input2);
        assert_eq!(steps2, vec![create_field_step("Blog", "field2", ScalarType::String)]);
    });
}

#[test]
fn special_handling_of_watch_migrations() {
    test_each_connector(|_, api| {
        let dm = r#"
            model Blog {
                id Int @id
            }
        "#;

        infer_and_apply_with_migration_id(api, &dm, "mig00");

        let dm = r#"
            model Blog {
                id Int @id
                field1 String
            }
        "#;

        infer_and_apply_with_migration_id(api, &dm, "watch01");

        let dm = r#"
            model Blog {
                id Int @id
                field1 String
                field2 String
            }
        "#;

        infer_and_apply_with_migration_id(api, &dm, "watch02");

        let dm = r#"
            model Blog {
                id Int @id
                field1 String
                field2 String
                field3 Int
            }
        "#;

        let input = InferMigrationStepsInput {
            migration_id: "mig02".to_string(),
            assume_to_be_applied: Vec::new(),
            datamodel: dm.to_string(),
        };

        let steps = run_infer_command(api, input);

        assert_eq!(
            steps,
            vec![
                create_field_step("Blog", "field1", ScalarType::String),
                create_field_step("Blog", "field2", ScalarType::String),
                create_field_step("Blog", "field3", ScalarType::Int),
            ]
        );
    });
}
