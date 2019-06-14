#![allow(non_snake_case)]
mod test_harness;
use test_harness::*;
use prisma_query::ast::*;

#[test]
fn adding_a_required_field_if_there_is_data() {
    test_each_connector(|sql_family, engine|{
        let dm = r#"
            model Test {
                id String @id
            }

            enum MyEnum {
                B
                A
            }
        "#;
        infer_and_apply(&engine, &dm);

        let conn = connectional(sql_family);
        let insert = Insert::single_into((SCHEMA_NAME, "Test")).value("id", "test");
        conn.execute_on_connection(SCHEMA_NAME, insert.into()).unwrap();

        let dm = r#"
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
                B
                A
            }
        "#;
        infer_and_apply(&engine, &dm);
    });
}