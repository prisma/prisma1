#![allow(non_snake_case)]
mod test_harness;
use database_inspector::*;
use migration_core::commands::*;
use migration_core::*;
use test_harness::*;

#[test]
fn adding_a_scalar_field_must_work() {
    run_test_with_engine(|engine| {
        let dm2 = r#"
            model Test {
                id: String @primary
                int: Int
                float: Float
                boolean: Boolean
                string: String
                dateTime: DateTime
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        let table = result.table_bang("Test");
        table.columns.iter().for_each(|c| assert_eq!(c.is_required, true));

        assert_eq!(table.column_bang("int").tpe, ColumnType::Int);
        assert_eq!(table.column_bang("float").tpe, ColumnType::Float);
        assert_eq!(table.column_bang("boolean").tpe, ColumnType::Boolean);
        assert_eq!(table.column_bang("string").tpe, ColumnType::String);
        assert_eq!(table.column_bang("dateTime").tpe, ColumnType::DateTime);
    });
}

#[test]
fn adding_an_optional_field_must_work() {
    run_test_with_engine(|engine| {
        let dm2 = r#"
            model Test {
                id: String @primary
                field: String?
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        let column = result.table_bang("Test").column_bang("field");
        assert_eq!(column.is_required, false);
    });
}

#[test]
fn adding_an_id_field_with_a_special_name_must_work() {
    run_test_with_engine(|engine| {
        let dm2 = r#"
            model Test {
                specialName: String @primary
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        let column = result.table_bang("Test").column("specialName");
        assert_eq!(column.is_some(), true);
    });
}

#[test]
fn removing_a_scalar_field_must_work() {
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model Test {
                id: String @primary
                field: String
            }
        "#;
        let result = migrate_to(&engine, &dm1);
        let column1 = result.table_bang("Test").column("field");
        assert_eq!(column1.is_some(), true);

        let dm2 = r#"
            model Test {
                id: String @primary
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        let column2 = result.table_bang("Test").column("field");
        assert_eq!(column2.is_some(), false);
    });
}

#[test]
#[ignore]
fn update_type_of_scalar_field_must_work() {
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model Test {
                id: String @primary
                field: String
            }
        "#;
        let result = migrate_to(&engine, &dm1);
        let column1 = result.table_bang("Test").column_bang("field");
        assert_eq!(column1.tpe, ColumnType::String);

        let dm2 = r#"
            model Test {
                id: String @primary
                field: Int
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        let column2 = result.table_bang("Test").column_bang("field");
        assert_eq!(column2.tpe, ColumnType::String);
    });
}

#[test]
#[ignore]
fn changing_the_type_of_an_id_field_must_work() {
    // TODO: this does not work because relation inlining is not supported yet.
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model A {
                id: Int @primary
                b: B # remove once implicit back relation field is implemented
            }
            model B {
                id: Int @primary
                a: A(id)
            }
        "#;
        let result = migrate_to(&engine, &dm1);
        let column = result.table_bang("B").column_bang("a");
        assert_eq!(column.tpe, ColumnType::Int);
        assert_eq!(column.foreign_key, Some(ForeignKey{table: "A".to_string(), column: "id".to_string()}));

        let dm2 = r#"
            model A {
                id: String @primary
                b: B # remove once implicit back relation field is implemented
            }
            model B {
                id: Int @primary
                a: A(id)
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        let column = result.table_bang("B").column_bang("a");
        // TODO: bring this back once barrel supports string ids
        // assert_eq!(column.tpe, ColumnType::String);
        assert_eq!(column.foreign_key, Some(ForeignKey{table: "A".to_string(), column: "id".to_string()}));
    });
}

#[test]
fn updating_db_name_of_a_scalar_field_must_work() {
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model A {
                id: String @primary
                field: String @db(name:"name1")
            }
        "#;
        let result = migrate_to(&engine, &dm1);
        assert_eq!(result.table_bang("A").column("name1").is_some(), true);

        let dm2 = r#"
            model A {
                id: String @primary
                field: String @db(name:"name2")
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        assert_eq!(result.table_bang("A").column("name1").is_some(), false);
        assert_eq!(result.table_bang("A").column("name2").is_some(), true);
    });
}

#[test]
fn changing_a_relation_field_to_a_scalar_field_must_work() {
    // this relies on link: INLINE which we don't support yet
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model A {
                id: Int @primary
                b: B(id)
            }
            model B {
                id: Int @primary
                a: A # remove this once the implicit back relation field is implemented
            }
        "#;
        let result = migrate_to(&engine, &dm1);
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.foreign_key.is_some(), true);
        assert_eq!(column.tpe, ColumnType::Int);

        let dm2 = r#"
            model A {
                id: Int @primary
                b: String
            }
            model B {
                id: Int @primary
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.foreign_key.is_some(), false);
        assert_eq!(column.tpe, ColumnType::String);
    });
}

#[test]
fn changing_a_scalar_field_to_a_relation_field_must_work() {
    // this relies on link: INLINE which we don't support yet
}

#[test]
fn adding_a_many_to_many_relation_must_result_in_a_prisma_style_relation_table() {
    // TODO: one model should have an id of different type. Not possible right now due to barrel limitation.
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model A {
                id: Int @primary
                bs: B[]
            }
            model B {
                id: Int @primary
                as: A[]
            }
        "#;
        let result = migrate_to(&engine, &dm1);
        let relation_table = result.table_bang("_AToB");
        assert_eq!(relation_table.columns.len(), 2);
        let aColumn = relation_table.column_bang("A");
        assert_eq!(aColumn.tpe, ColumnType::Int);
        assert_eq!(aColumn.foreign_key, Some(ForeignKey{table: "A".to_string(), column: "id".to_string()}));
        let bColumn = relation_table.column_bang("B");
        assert_eq!(bColumn.tpe, ColumnType::Int);
        assert_eq!(bColumn.foreign_key, Some(ForeignKey{table: "B".to_string(), column: "id".to_string()}));
    });
}

#[test]
#[ignore]
fn adding_a_many_to_many_relation_for_exotic_id_types_must_work() {
    // TODO: add this once we have figured out what id types we support
    unimplemented!();
}

#[test]
#[ignore]
fn forcing_a_relation_table_for_a_one_to_many_relation_must_work() {
    // TODO: implement this once we have decided if this is actually possible in dm v2
    unimplemented!();
}

// #[test]
// #[ignore]
// fn forcing_a_relation_table_for_a_one_to_many_relation_must_work() {
//     // TODO: implement this once we have decided if this is actually possible in dm v2
//     unimplemented!();
// }

#[test]
#[ignore]
fn providing_an_explicit_link_table_must_work() {
     // TODO: implement this once we have decided if this is actually possible in dm v2
    unimplemented!();
}

#[test]
#[ignore]
fn adding_a_unique_constraint_must_work() {
    // TODO: bring back when index introspection is implemented
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model A {
                id: Int @primary
                field: String @unique
            }
        "#;
        let result = dbg!(migrate_to(&engine, &dm1));
        let index = result.table_bang("A").indexes.iter().find(|i|i.columns == vec!["field"]);
        assert_eq!(index.is_some(), true);
        assert_eq!(index.unwrap().unique, true);
    });
}

#[test]
#[ignore]
fn removing_a_unique_constraint_must_work() {
    // TODO: bring back when index introspection is implemented
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model A {
                id: Int @primary
                field: String @unique
            }
        "#;
        let result = migrate_to(&engine, &dm1);
        let index = result.table_bang("A").indexes.iter().find(|i|i.columns == vec!["field"]);
        assert_eq!(index.is_some(), true);
        assert_eq!(index.unwrap().unique, true);

        let dm2 = r#"
            model A {
                id: Int @primary
            }
        "#;
        let result = dbg!(migrate_to(&engine, &dm2));
        let index = result.table_bang("A").indexes.iter().find(|i|i.columns == vec!["field"]);
        assert_eq!(index.is_some(), false);
    });
}

#[test]
fn adding_a_scalar_list_for_a_modelwith_id_type_int_must_work() {
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model A {
                id: Int @primary
                strings: String[]
            }
        "#;
        let result = migrate_to(&engine, &dm1);
        let scalar_list_table = result.table_bang("A_strings");
        let node_id_column = scalar_list_table.column_bang("nodeId");
        assert_eq!(node_id_column.tpe, ColumnType::Int);
    });
}



fn migrate_to(engine: &Box<MigrationEngine>, datamodel: &str) -> DatabaseSchema {
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
    let output = cmd.execute(&engine);

    let inspector = engine.connector().database_inspector();
    inspector.introspect(&engine.schema_name())
}
