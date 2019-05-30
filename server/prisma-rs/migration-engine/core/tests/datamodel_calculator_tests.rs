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
    let dm1 = Schema::empty();
    let dm2 = parse(
        r#"
        model Test {
            id: String @id
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
            id: String
        }
    "#,
    );
    let dm2 = Schema::empty();

    test(dm1, dm2);
}

#[test]
fn add_UpdateModel_to_existing_schema() {
    let dm1 = parse(
        r#"
        model Post {
            id: String
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Post {
            id: String
        }
        @embedded
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_CreateField_to_existing_schema() {
    let dm1 = parse(
        r#"
        model Test {
            id: String
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Test {
            id: String
            field: Int?
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
            id: String
        }
        model Post {
            id: String
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Blog {
            id: String
            posts: Post[]
        }
        model Post {
            id: String
            blog: Blog?
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
            id: String
            field: Int?
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Test {
            id: String
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
            id: String
            field: Int?
        }
    "#,
    );
    let dm2 = parse(
        r#"
        model Test {
            id: String
            field: Boolean @default(false)
        }
    "#,
    );

    test(dm1, dm2);
}

#[test]
fn add_CreateEnum_to_existing_schema() {
    let dm1 = Schema::empty();
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
    let dm2 = Schema::empty();

    test(dm1, dm2);
}

// This tests use inferrer to create an end-to-end situation.
fn test(dm1: Schema, dm2: Schema) {
    let steps = infer(&dm1, &dm2);
    let result = calculate(&dm1, steps);
    assert_eq!(dm2, result);
}
fn calculate(schema: &Schema, steps: Vec<MigrationStep>) -> Schema {
    let calc = DataModelCalculatorImpl {};
    calc.infer(schema, &steps)
}

fn infer(dm1: &Schema, dm2: &Schema) -> Vec<MigrationStep> {
    let inferrer = DataModelMigrationStepsInferrerImplWrapper {};
    inferrer.infer(dm1, dm2)
}
