mod common;
use barrel::types;
use common::*;
use database_inspector::{
    sql::{sqlite::*, *},
    IntrospectionConnector,
};
mod relational_asserts;
use relational_asserts::*;

#[test]
fn all_columns_types_must_work() {
    run_test(
        |migration| {
            migration.create_table("User", |t| {
                t.add_column("int", types::integer());
                t.add_column("float", types::float());
                t.add_column("boolean", types::boolean());
                t.add_column("string1", types::text());
                t.add_column("string2", types::varchar(1));
                t.add_column("date_time", types::date());
            });
        },
        |connection| {
            let inspector = SqlIntrospectionConnector::new(Box::new(SqliteConnector::new()));

            let result = inspector.introspect(connection, SCHEMA)?;

            let user_table = result.schema.assert_has_table("User");

            user_table.assert_has_pk(false);

            user_table.assert_has_column("int").assert_column_type(ColumnType::Int);

            user_table
                .assert_has_column("float")
                .assert_column_type(ColumnType::Float);

            user_table
                .assert_has_column("boolean")
                .assert_column_type(ColumnType::Boolean);

            user_table
                .assert_has_column("string1")
                .assert_column_type(ColumnType::String);

            user_table
                .assert_has_column("string2")
                .assert_column_type(ColumnType::String);

            user_table
                .assert_has_column("date_time")
                .assert_column_type(ColumnType::DateTime);

            Ok(())
        },
    )
    .unwrap();
}

#[test]
fn is_required_must_work() {
    run_test(
        |migration| {
            migration.create_table("User", |t| {
                t.add_column("column1", types::integer().nullable(false));
                t.add_column("column2", types::integer().nullable(true));
            });
        },
        |connection| {
            let inspector = SqlIntrospectionConnector::new(Box::new(SqliteConnector::new()));

            let result = inspector.introspect(connection, SCHEMA)?;

            let user_table = result.schema.assert_has_table("User");
            user_table.assert_has_pk(false);

            user_table.assert_has_column("column1").assert_is_nullable(false);

            user_table.assert_has_column("column2").assert_is_nullable(true);

            Ok(())
        },
    )
    .unwrap();
}

#[test]
fn foreign_keys_must_work() {
    run_test(
        |migration| {
            migration.create_table("City", |t| {
                t.add_column("id", types::primary());
            });
            migration.create_table("User", |t| {
                t.add_column("city", types::foreign("City(id)"));
            });
        },
        |connection| {
            let inspector = SqlIntrospectionConnector::new(Box::new(SqliteConnector::new()));
            let result = inspector.introspect(connection, SCHEMA)?;

            result.schema.assert_has_relation("User", "city", "City", "id");

            Ok(())
        },
    )
    .unwrap();
}

#[test]
fn primary_key_indices_must_work() {
    run_test(
        |migration| {
            migration.create_table("City", |t| {
                t.add_column("id", types::primary());
            });
        },
        |connection| {
            let inspector = SqlIntrospectionConnector::new(Box::new(SqliteConnector::new()));
            let result = inspector.introspect(connection, SCHEMA)?;

            let city_table = result.schema.assert_has_table("City");
            city_table.assert_has_pk(true);

            city_table
                .assert_has_column("id")
                .assert_column_type(ColumnType::Int)
                .assert_is_primary_key(true);

            city_table.assert_has_pk_columns(&["id"]);

            Ok(())
        },
    )
    .unwrap();
}
