#![allow(non_snake_case)]
mod test_harness;
use datamodel::dml::*;
use migration_connector::steps::*;
use migration_core::migration::datamodel_calculator::*;
use migration_core::migration::datamodel_migration_steps_inferrer::*;
use test_harness::parse;

// TODO: We could unify the tests for inferrer and calculator.

#[test]
fn add_CreateModel_to_existing_schema() {
    let dm1 = Datamodel::empty();
    let dm2 = parse(
        r#"
        model Test {
            id String @id @default(cuid())
        }
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_DeleteModel_to_existing_schema() {
    let dm1 = parse(
        r#"
        model Test {
            id String @id @default(cuid())
        }
    "#,
    );
    let dm2 = Datamodel::empty();

    test(dm1, dm2);
}

#[test]
fn add_UpdateModel_to_existing_schema() {
    let dm1 = parse(
        r#"
        model Post {
            id String @id @default(cuid())
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Post {
            id String @id @default(cuid())
            
            @@embedded
        }
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_CreateField_to_existing_schema() {
    let dm1 = parse(
        r#"
        model Test {
            id String @id @default(cuid())
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Test {
            id String @id @default(cuid())
            field Int?
        }
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_CreateField_for_relation_to_existing_schema() {
    let dm1 = parse(
        r#"
        model Blog {
            id String @id @default(cuid())
        }
        model Post {
            id String @id @default(cuid())
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Blog {
            id String @id @default(cuid())
            posts Post[]
        }
        model Post {
            id String @id @default(cuid())
            blog Blog?
        }
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_DeleteField_to_existing_schema() {
    let dm1 = parse(
        r#"
        model Test {
            id String @id @default(cuid())
            field Int?
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Test {
            id String @id @default(cuid())
        }
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_UpdateField_to_existing_schema() {
    let dm1 = parse(
        r#"
        model Test {
            id String @id @default(cuid())
            field Int?
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Test {
            id String @id @default(cuid())
            field Boolean @default(false)
        }
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_CreateEnum_to_existing_schema() {
    let dm1 = Datamodel::empty();
    let dm2 = parse(
        r#"
        enum Test {
            A
            B
        }
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_DeleteEnum_to_existing_schema() {
    let dm1 = parse(
        r#"
        enum Test {
            A
            B
        }
    "#,
    );
    let dm2 = Datamodel::empty();

    test(dm1, dm2);
}

#[should_panic(
    expected = "The model Test already exists in this Datamodel. It is not possible to create it once more."
)]
#[test]
fn creating_a_model_that_already_exists_must_error() {
    let dm = parse(
        r#"
            model Test {
                id Int @id
            }
        "#,
    );

    let steps = vec![MigrationStep::CreateModel(CreateModel {
        name: "Test".to_string(),
        db_name: None,
        embedded: false,
    })];

    calculate(&dm, steps);
}

#[should_panic(
    expected = "The field id on model Test already exists in this Datamodel. It is not possible to create it once more."
)]
#[test]
fn creating_a_field_that_already_exists_must_error() {
    let dm = parse(
        r#"
            model Test {
                id Int @id
            }
        "#,
    );

    let steps = vec![MigrationStep::CreateField(CreateField {
        model: "Test".to_string(),
        name: "id".to_string(),
        tpe: FieldType::Base(ScalarType::Int),
        arity: FieldArity::Required,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: false,
        id: None,
        default: None,
        scalar_list: None,
    })];

    calculate(&dm, steps);
}

#[should_panic(expected = "The enum Test already exists in this Datamodel. It is not possible to create it once more.")]
#[test]
fn creating_an_enum_that_already_exists_must_error() {
    let dm = parse(
        r#"
            enum Test {
                A
                B
            }
        "#,
    );

    let steps = vec![MigrationStep::CreateEnum(CreateEnum {
        name: "Test".to_string(),
        values: Vec::new(),
        db_name: None,
    })];

    calculate(&dm, steps);
}

#[should_panic(expected = "The model Test does not exist in this Datamodel. It is not possible to delete it.")]
#[test]
fn deleting_a_model_that_does_not_exist_must_error() {
    let dm = Datamodel::empty();
    let steps = vec![MigrationStep::DeleteModel(DeleteModel {
        name: "Test".to_string(),
    })];

    calculate(&dm, steps);
}

#[should_panic(
    expected = "The model Test does not exist in this Datamodel. It is not possible to delete a field in it."
)]
#[test]
fn deleting_a_field_that_does_not_exist_must_error() {
    let dm = Datamodel::empty();
    let steps = vec![MigrationStep::DeleteField(DeleteField {
        model: "Test".to_string(),
        name: "id".to_string(),
    })];

    calculate(&dm, steps);
}

#[should_panic(
    expected = "The field my_field on model Test does not exist in this Datamodel. It is not possible to delete it."
)]
#[test]
fn deleting_a_field_that_does_not_exist_2_must_error() {
    let dm = parse(
        r#"
            model Test {
                id Int @id
            }
        "#,
    );
    let steps = vec![MigrationStep::DeleteField(DeleteField {
        model: "Test".to_string(),
        name: "my_field".to_string(),
    })];

    calculate(&dm, steps);
}

#[should_panic(expected = "The enum Test does not exist in this Datamodel. It is not possible to delete it.")]
#[test]
fn deleting_an_enum_that_does_not_exist_must_error() {
    let dm = Datamodel::empty();
    let steps = vec![MigrationStep::DeleteEnum(DeleteEnum {
        name: "Test".to_string(),
    })];

    calculate(&dm, steps);
}

#[should_panic(expected = "The model Test does not exist in this Datamodel. It is not possible to update it.")]
#[test]
fn updating_a_model_that_does_not_exist_must_error() {
    let dm = Datamodel::empty();
    let steps = vec![MigrationStep::UpdateModel(UpdateModel {
        name: "Test".to_string(),
        new_name: None,
        db_name: None,
        embedded: None,
    })];

    calculate(&dm, steps);
}

#[should_panic(
    expected = "The model Test does not exist in this Datamodel. It is not possible to update a field in it."
)]
#[test]
fn updating_a_field_that_does_not_exist_must_error() {
    let dm = Datamodel::empty();
    let steps = vec![MigrationStep::UpdateField(UpdateField {
        model: "Test".to_string(),
        name: "id".to_string(),
        new_name: None,
        tpe: None,
        arity: None,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: None,
        id_info: None,
        default: None,
        scalar_list: None,
    })];

    calculate(&dm, steps);
}

#[should_panic(
    expected = "The field myField on model Test does not exist in this Datamodel. It is not possible to update it."
)]
#[test]
fn updating_a_field_that_does_not_exist_must_error_2() {
    let dm = parse(
        r#"
            model Test {
                id Int @id
            }
        "#,
    );
    let steps = vec![MigrationStep::UpdateField(UpdateField {
        model: "Test".to_string(),
        name: "myField".to_string(),
        new_name: None,
        tpe: None,
        arity: None,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: None,
        id_info: None,
        default: None,
        scalar_list: None,
    })];

    calculate(&dm, steps);
}

#[should_panic(expected = "The enum Test does not exist in this Datamodel. It is not possible to update it.")]
#[test]
fn updating_an_enum_that_does_not_exist_must_error() {
    let dm = Datamodel::empty();
    let steps = vec![MigrationStep::UpdateEnum(UpdateEnum {
        name: "Test".to_string(),
        new_name: None,
        values: None,
        db_name: None,
    })];

    calculate(&dm, steps);
}

// This tests use inferrer to create an end-to-end situation.
fn test(dm1: Datamodel, dm2: Datamodel) {
    let steps = infer(&dm1, &dm2);
    let result = calculate(&dm1, steps);
    assert_eq!(dm2, result);
}
fn calculate(schema: &Datamodel, steps: Vec<MigrationStep>) -> Datamodel {
    let calc = DataModelCalculatorImpl {};
    calc.infer(schema, &steps)
}

fn infer(dm1: &Datamodel, dm2: &Datamodel) -> Vec<MigrationStep> {
    let inferrer = DataModelMigrationStepsInferrerImplWrapper {};
    inferrer.infer(dm1, dm2)
}
