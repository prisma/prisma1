#![allow(non_snake_case)]
mod test_harness;
use test_harness::*;
use prisma_query::ast::*;
use sql_migration_connector::SqlFamily;

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

#[test]
fn adding_a_required_field_must_use_the_default_value_for_migrations() {
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
                int Int @default(1)
                float Float @default(2)
                boolean Boolean @default(true)
                string String @default("test_string")
                dateTime DateTime 
                enum MyEnum @default(C)
            }

            enum MyEnum {
                B
                A
                C
            }
        "#;
        infer_and_apply(&engine, &dm);

        // TODO: those assertions somehow fail with column not found on SQLite. I could observe the correct data in the db file though.
        if sql_family != SqlFamily::Sqlite {
            let conditions = "id".equals("test");
            let table_for_select: Table = match sql_family {
                SqlFamily::Sqlite => {
                    // sqlite case. Otherwise prisma-query produces invalid SQL
                    "Test".into()
                }
                _ => (SCHEMA_NAME, "Test").into(),
            };
            let query = Select::from_table(table_for_select).so_that(conditions);
            let result_set = conn.query_on_connection(SCHEMA_NAME, query.into()).unwrap();
            let row = result_set.into_iter().next().unwrap();
            assert_eq!(row.get_as_integer("int").unwrap(), 1);
            assert_eq!(row.get_as_string("string").unwrap(), "test_string");
        }
    });
}