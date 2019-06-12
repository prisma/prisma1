#![allow(non_snake_case)]
#![allow(unused)]

use barrel::{backend::Sqlite as Squirrel, types, Migration};
use database_inspector::*;
use prisma_query::connector::Sqlite as SqliteDatabaseClient;
use prisma_query::{Connectional, ResultSet};
use std::{thread, time};

const SCHEMA: &str = "database_inspector_test";

#[test]
fn all_columns_types_must_work() {
    let inspector = setup(|mut migration| {
        migration.create_table("User", |t| {
            t.add_column("int", types::integer());
            t.add_column("float", types::float());
            t.add_column("boolean", types::boolean());
            t.add_column("string1", types::text());
            t.add_column("string2", types::varchar(1));
            t.add_column("date_time", types::date());
        });
    });

    let result = inspector.introspect(&SCHEMA.to_string());

    let table = result.table("User").unwrap();
    let expected_columns = vec![
        Column {
            name: "int".to_string(),
            tpe: ColumnType::Int,
            is_required: true,
            foreign_key: None,
            sequence: None,
        },
        Column {
            name: "float".to_string(),
            tpe: ColumnType::Float,
            is_required: true,
            foreign_key: None,
            sequence: None,
        },
        Column {
            name: "boolean".to_string(),
            tpe: ColumnType::Boolean,
            is_required: true,
            foreign_key: None,
            sequence: None,
        },
        Column {
            name: "string1".to_string(),
            tpe: ColumnType::String,
            is_required: true,
            foreign_key: None,
            sequence: None,
        },
        Column {
            name: "string2".to_string(),
            tpe: ColumnType::String,
            is_required: true,
            foreign_key: None,
            sequence: None,
        },
        Column {
            name: "date_time".to_string(),
            tpe: ColumnType::DateTime,
            is_required: true,
            foreign_key: None,
            sequence: None,
        },
    ];

    assert_eq!(table.columns, expected_columns);
}

#[test]
fn is_required_must_work() {
    let inspector = setup(|mut migration| {
        migration.create_table("User", |t| {
            t.add_column("column1", types::integer().nullable(false));
            t.add_column("column2", types::integer().nullable(true));
        });
    });

    let result = inspector.introspect(&SCHEMA.to_string());

    let user_table = result.table("User").unwrap();
    let expected_columns = vec![
        Column {
            name: "column1".to_string(),
            tpe: ColumnType::Int,
            is_required: true,
            foreign_key: None,
            sequence: None,
        },
        Column {
            name: "column2".to_string(),
            tpe: ColumnType::Int,
            is_required: false,
            foreign_key: None,
            sequence: None,
        },
    ];
    assert_eq!(user_table.columns, expected_columns);
}

#[test]
fn foreign_keys_must_work() {
    let inspector = setup(|mut migration| {
        migration.create_table("City", |t| {
            t.add_column("id", types::primary());
        });
        migration.create_table("User", |t| {
            t.add_column("city", types::foreign("City(id)"));
        });
    });

    let result = inspector.introspect(&SCHEMA.to_string());

    let user_table = result.table("User").unwrap();
    let expected_columns = vec![Column {
        name: "city".to_string(),
        tpe: ColumnType::Int,
        is_required: true,
        foreign_key: Some(ForeignKey {
            table: "City".to_string(),
            column: "id".to_string(),
        }),
        sequence: None,
    }];
    assert_eq!(user_table.columns, expected_columns);
}

fn setup<F>(mut migrationFn: F) -> Box<DatabaseInspector>
where
    F: FnMut(&mut Migration) -> (),
{    
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let database_folder_path = format!("{}/db", server_root);
    let database_file_path = dbg!(format!("{}/{}.db", database_folder_path, SCHEMA));
    let _ = std::fs::remove_file(database_file_path.clone()); // ignore potential errors

    let inspector = DatabaseInspector::sqlite(database_file_path);
    let mut migration = Migration::new().schema(SCHEMA);
    migrationFn(&mut migration);
    let full_sql = migration.make::<Squirrel>();
    for sql in full_sql.split(";") {
        dbg!(sql);
        if sql != "" {
            inspector.connectional.query_on_raw_connection(&SCHEMA, &sql, &[]).unwrap();
        }
    }
    Box::new(inspector)
}
