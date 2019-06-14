#![allow(non_snake_case)]
mod test_harness;
use sql_migration_connector::database_inspector::*;
use test_harness::*;

#[test]
fn adding_a_scalar_field_must_work() {
    test_each_connector(|_,engine| {
        let dm2 = r#"
            model Test {
                id String @id
                int Int
                float Float
                boolean Boolean
                string String
                dateTime DateTime
                enum MyEnum
            }

            enum MyEnum {
                A
                B
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let table = result.table_bang("Test");
        table.columns.iter().for_each(|c| assert_eq!(c.is_required, true));

        assert_eq!(table.column_bang("int").tpe, ColumnType::Int);
        assert_eq!(table.column_bang("float").tpe, ColumnType::Float);
        assert_eq!(table.column_bang("boolean").tpe, ColumnType::Boolean);
        assert_eq!(table.column_bang("string").tpe, ColumnType::String);
        assert_eq!(table.column_bang("dateTime").tpe, ColumnType::DateTime);
        assert_eq!(table.column_bang("enum").tpe, ColumnType::String);
    });
}
//
//#[test]
//fn apply_schema() {
//    test_each_connector(|engine| {
//        let dm2 = r#"
//            model Test {
//                id String @id
//                int Int
//                float Float
//                boolean Boolean
//                string String
//                dateTime DateTime
//                enum MyEnum
//            }
//
//            enum MyEnum {
//                A
//                B
//            }
//        "#;
//
//        infer_and_apply(&engine, &dm2);
//    });
//}

#[test]
fn adding_an_optional_field_must_work() {
    test_each_connector(|_,engine| {
        let dm2 = r#"
            model Test {
                id String @id
                field String?
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column = result.table_bang("Test").column_bang("field");
        assert_eq!(column.is_required, false);
    });
}

#[test]
fn adding_an_id_field_with_a_special_name_must_work() {
    test_each_connector(|_,engine| {
        let dm2 = r#"
            model Test {
                specialName String @id
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column = result.table_bang("Test").column("specialName");
        assert_eq!(column.is_some(), true);
    });
}

#[test]
fn removing_a_scalar_field_must_work() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model Test {
                id String @id
                field String
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let column1 = result.table_bang("Test").column("field");
        assert_eq!(column1.is_some(), true);

        let dm2 = r#"
            model Test {
                id String @id
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column2 = result.table_bang("Test").column("field");
        assert_eq!(column2.is_some(), false);
    });
}

#[test]
fn can_handle_reserved_sql_keywords_for_model_name() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model Group {
                id String @id
                field String
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let column = result.table_bang("Group").column_bang("field");
        assert_eq!(column.tpe, ColumnType::String);

        let dm2 = r#"
            model Group {
                id String @id
                field Int
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column = result.table_bang("Group").column_bang("field");
        assert_eq!(column.tpe, ColumnType::Int);
    });
}

#[test]
fn can_handle_reserved_sql_keywords_for_field_name() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model Test {
                id String @id
                Group String
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let column = result.table_bang("Test").column_bang("Group");
        assert_eq!(column.tpe, ColumnType::String);

        let dm2 = r#"
            model Test {
                id String @id
                Group Int
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column = result.table_bang("Test").column_bang("Group");
        assert_eq!(column.tpe, ColumnType::Int);
    });
}

#[test]
fn update_type_of_scalar_field_must_work() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model Test {
                id String @id
                field String
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let column1 = result.table_bang("Test").column_bang("field");
        assert_eq!(column1.tpe, ColumnType::String);

        let dm2 = r#"
            model Test {
                id String @id
                field Int
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column2 = result.table_bang("Test").column_bang("field");
        assert_eq!(column2.tpe, ColumnType::Int);
    });
}

#[test]
fn changing_the_type_of_an_id_field_must_work() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                b  B   @relation(references: [id])
            }
            model B {
                id Int @id
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.tpe, ColumnType::Int);
        assert_eq!(
            column.foreign_key,
            Some(ForeignKey {
                table: "B".to_string(),
                column: "id".to_string()
            })
        );

        let dm2 = r#"
            model A {
                id Int @id
                b  B   @relation(references: [id])
            }
            model B {
                id String @id @default(cuid())
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.tpe, ColumnType::String);
        assert_eq!(
            column.foreign_key,
            Some(ForeignKey {
                table: "B".to_string(),
                column: "id".to_string()
            })
        );
    });
}

#[test]
fn updating_db_name_of_a_scalar_field_must_work() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id String @id
                field String @map(name:"name1")
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        assert_eq!(result.table_bang("A").column("name1").is_some(), true);

        let dm2 = r#"
            model A {
                id String @id
                field String @map(name:"name2")
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        assert_eq!(result.table_bang("A").column("name1").is_some(), false);
        assert_eq!(result.table_bang("A").column("name2").is_some(), true);
    });
}

#[test]
fn changing_a_relation_field_to_a_scalar_field_must_work() {
    // this relies on link: INLINE which we don't support yet
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                b B @relation(references: [id])
            }
            model B {
                id Int @id
                a A // remove this once the implicit back relation field is implemented
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.foreign_key.is_some(), true);
        assert_eq!(column.tpe, ColumnType::Int);

        let dm2 = r#"
            model A {
                id Int @id
                b String
            }
            model B {
                id Int @id
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.foreign_key.is_some(), false);
        assert_eq!(column.tpe, ColumnType::String);
    });
}

#[test]
fn changing_a_scalar_field_to_a_relation_field_must_work() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                b String
            }
            model B {
                id Int @id
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.foreign_key.is_some(), false);
        assert_eq!(column.tpe, ColumnType::String);

        let dm2 = r#"
            model A {
                id Int @id
                b B @relation(references: [id])
            }
            model B {
                id Int @id
                a A // remove this once the implicit back relation field is implemented
            }
        "#;
        let result = infer_and_apply(&engine, &dm2);
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.foreign_key.is_some(), true);
        assert_eq!(column.tpe, ColumnType::Int);
    });
}

#[test]
fn adding_a_many_to_many_relation_must_result_in_a_prisma_style_relation_table() {
    // TODO: one model should have an id of different type. Not possible right now due to barrel limitation.
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                bs B[]
            }
            model B {
                id Int @id
                as A[]
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let relation_table = result.table_bang("_AToB");
        assert_eq!(relation_table.columns.len(), 2);
        let aColumn = relation_table.column_bang("A");
        assert_eq!(aColumn.tpe, ColumnType::Int);
        assert_eq!(
            aColumn.foreign_key,
            Some(ForeignKey {
                table: "A".to_string(),
                column: "id".to_string()
            })
        );
        let bColumn = relation_table.column_bang("B");
        assert_eq!(bColumn.tpe, ColumnType::Int);
        assert_eq!(
            bColumn.foreign_key,
            Some(ForeignKey {
                table: "B".to_string(),
                column: "id".to_string()
            })
        );
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
fn adding_an_inline_relation_must_result_in_a_foreign_key_in_the_model_table() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                b B @relation(references: [id])
            }

            model B {
                id Int @id
                a A // todo: remove when implicit back relation field is implemented
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm1));
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.tpe, ColumnType::Int);
        assert_eq!(
            column.foreign_key,
            Some(ForeignKey {
                table: "B".to_string(),
                column: "id".to_string()
            })
        );
    });
}

#[test]
fn specifying_a_db_name_for_an_inline_relation_must_work() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                b B @relation(references: [id]) @map(name: "b_column")
            }

            model B {
                id Int @id
                a A // todo: remove when implicit back relation field is implemented
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm1));
        let column = result.table_bang("A").column_bang("b_column");
        assert_eq!(column.tpe, ColumnType::Int);
        assert_eq!(
            column.foreign_key,
            Some(ForeignKey {
                table: "B".to_string(),
                column: "id".to_string()
            })
        );
    });
}

#[test]
fn adding_an_inline_relation_to_a_model_with_an_exotic_id_type() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                b B @relation(references: [id])
            }

            model B {
                id String @id
                a A // todo: remove when implicit back relation field is implemented
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm1));
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(column.tpe, ColumnType::String);
        assert_eq!(
            column.foreign_key,
            Some(ForeignKey {
                table: "B".to_string(),
                column: "id".to_string()
            })
        );
    });
}

#[test]
fn removing_an_inline_relation_must_work() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                b B @relation(references: [id])
            }

            model B {
                id Int @id
                a A // todo: remove when implicit back relation field is implemented
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm1));
        let column = result.table_bang("A").column("b");
        assert_eq!(column.is_some(), true);

        let dm2 = r#"
            model A {
                id Int @id
            }

            model B {
                id Int @id
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm2));
        let column = result.table_bang("A").column("b");
        assert_eq!(column.is_some(), false);
    });
}

#[test]
fn moving_an_inline_relation_to_the_other_side_must_work() {
    // TODO: bring this back when relation inlining works in the new datamodel
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                b B @relation(references: [id])
            }

            model B {
                id Int @id
                a A // todo: remove when implicit back relation field is implemented
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm1));
        let column = result.table_bang("A").column_bang("b");
        assert_eq!(
            column.foreign_key,
            Some(ForeignKey {
                table: "B".to_string(),
                column: "id".to_string()
            })
        );

        let dm2 = r#"
            model A {
                id Int @id
                b B // todo: remove when implicit back relation field is implemented
            }

            model B {
                id Int @id
                a A @relation(references: [id])
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm2));
        let column = result.table_bang("B").column_bang("a");
        assert_eq!(
            column.foreign_key,
            Some(ForeignKey {
                table: "A".to_string(),
                column: "id".to_string()
            })
        );
    });
}

#[test]
#[ignore]
fn adding_a_unique_constraint_must_work() {
    // TODO: bring back when index introspection is implemented
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                field String @unique
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm1));
        let index = result
            .table_bang("A")
            .indexes
            .iter()
            .find(|i| i.columns == vec!["field"]);
        assert_eq!(index.is_some(), true);
        assert_eq!(index.unwrap().unique, true);
    });
}

#[test]
#[ignore]
fn removing_a_unique_constraint_must_work() {
    // TODO: bring back when index introspection is implemented
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                field String @unique
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let index = result
            .table_bang("A")
            .indexes
            .iter()
            .find(|i| i.columns == vec!["field"]);
        assert_eq!(index.is_some(), true);
        assert_eq!(index.unwrap().unique, true);

        let dm2 = r#"
            model A {
                id Int @id
            }
        "#;
        let result = dbg!(infer_and_apply(&engine, &dm2));
        let index = result
            .table_bang("A")
            .indexes
            .iter()
            .find(|i| i.columns == vec!["field"]);
        assert_eq!(index.is_some(), false);
    });
}

#[test]
fn adding_a_scalar_list_for_a_modelwith_id_type_int_must_work() {
    test_each_connector(|_,engine| {
        let dm1 = r#"
            model A {
                id Int @id
                strings String[]
            }
        "#;
        let result = infer_and_apply(&engine, &dm1);
        let scalar_list_table = result.table_bang("A_strings");
        let node_id_column = scalar_list_table.column_bang("nodeId");
        assert_eq!(node_id_column.tpe, ColumnType::Int);
        assert_eq!(scalar_list_table.primary_key_columns, vec!["nodeId", "position"]);
    });
}

#[test]
fn updating_a_model_with_a_scalar_list_to_a_different_id_type_must_work() {
    test_each_connector(|_,engine| {
        let dm = r#"
            model A {
                id Int @id
                strings String[]
            }
        "#;
        let result = infer_and_apply(&engine, &dm);
        let node_id_column = result.table_bang("A_strings").column_bang("nodeId");
        assert_eq!(node_id_column.tpe, ColumnType::Int);

        let dm = r#"
            model A {
                id String @id
                strings String[]
            }
        "#;
        let result = infer_and_apply(&engine, &dm);
        let node_id_column = result.table_bang("A_strings").column_bang("nodeId");
        assert_eq!(node_id_column.tpe, ColumnType::String);
    });
}
