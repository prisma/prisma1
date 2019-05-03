#![allow(non_snake_case)]

use migration_core::migration::datamodel_migration_steps_inferrer::{
    DataModelMigrationStepsInferrer, DataModelMigrationStepsInferrerImpl,
};
use migration_core::steps::*;
use prisma_datamodel::dml::*;
use prisma_datamodel::Validator;

#[test]
#[ignore]
fn infer_CreateModel_if_it_does_not_exit_yet() {
    let dm1 = Schema::empty();
    let dm2 = parse(
        r#"
        type Test {
            id: ID
        }
    "#,
    );

    let steps = infer(dm1, dm2);
    let expected = vec![
        MigrationStep::CreateModel(CreateModel {
            name: "Test".to_string(),
            db_name: None,
            embedded: None,
        }),
        MigrationStep::CreateField(CreateField {
            model: "Test".to_string(),
            name: "id".to_string(),
            ..Default::default()
        }),
    ];
    assert_eq!(steps, expected);
}

#[test]
#[ignore]
fn infer_CreateField_if_it_does_not_exist_yet() {
    let dm1 = parse(
        r#"
        type Test {
            id: ID
        }
    "#,
    );
    let dm2 = parse(
        r#"
        type Test {
            id: ID
            field: Int
        }
    "#,
    );

    let steps = infer(dm1, dm2);
    let expected = vec![MigrationStep::CreateField(CreateField {
        model: "Test".to_string(),
        name: "id".to_string(),
        tpe: "Int".to_string(),
        ..Default::default()
    })];
    assert_eq!(steps, expected);
}

// TODO: we will need this in a lot of test files. Extract it.
fn parse(datamodel_string: &'static str) -> Schema {
    let ast = prisma_datamodel::parser::parse(&datamodel_string.to_string());
    // TODO: this would need capabilities
    let validator = Validator::new();
    validator.validate(&ast)
}

fn infer(dm1: Schema, dm2: Schema) -> Vec<MigrationStep> {
    DataModelMigrationStepsInferrerImpl::infer(dm1, dm2)
}
