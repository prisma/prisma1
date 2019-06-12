#![allow(non_snake_case)]
#![allow(unused)]

use barrel::{ types, Migration};
use database_inspector::*;
use prisma_query::connector::Sqlite as SqliteDatabaseClient;
use prisma_query::{Connectional, ResultSet};
use std::{thread, time};
use std::sync::Arc;

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
    test_each_backend(|mut migration| {
        migration.create_table("City", |t| {
            t.add_column("id", types::primary());
        });
        migration.create_table("User", |t| {
            t.add_column("city", types::foreign("City(id)"));
        });
    }, |inspector|{
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
    });

}

fn test_each_backend<MigrationFn, TestFn>(mut migrationFn: MigrationFn, testFn: TestFn)
    where 
        MigrationFn: FnMut(&mut Migration) -> (),
        TestFn: Fn(Arc<DatabaseInspector>) -> (),
{
    let mut migration = Migration::new().schema(SCHEMA);
    migrationFn(&mut migration);

    println!("Testing with SQLite now");
    // SQLITE
    {
        let (inspector, connectional) = sqlite();
        let full_sql = migration.make::<barrel::backend::Sqlite>();
        run_full_sql(&connectional, &full_sql);
        testFn(inspector);
    }
    println!("Testing with Postgres now");
    // POSTGRES
    {
        let (inspector, connectional) = postgres();
        let full_sql = migration.make::<barrel::backend::Pg>();
        run_full_sql(&connectional, &full_sql);
        testFn(inspector);
    }
}

fn run_full_sql(connectional: &Arc<Connectional>, full_sql: &str) {
    for sql in full_sql.split(";") {
        dbg!(sql);
        if sql != "" {
            connectional.query_on_raw_connection(&SCHEMA, &sql, &[]).unwrap();
        }
    }
}

fn setup<F>(mut migrationFn: F) -> Arc<DatabaseInspector>
where
    F: FnMut(&mut Migration) -> (),
{        
    let mut migration = Migration::new().schema(SCHEMA);
    migrationFn(&mut migration);
    let (inspector, connectional) = sqlite();
    let full_sql = migration.make::<barrel::backend::Sqlite>();
    for sql in full_sql.split(";") {
        dbg!(sql);
        if sql != "" {
            connectional.query_on_raw_connection(&SCHEMA, &sql, &[]).unwrap();
        }
    }
    inspector
}

fn sqlite() -> (Arc<DatabaseInspector>, Arc<Connectional>) {
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let database_folder_path = format!("{}/db", server_root);
    let database_file_path = dbg!(format!("{}/{}.db", database_folder_path, SCHEMA));
    let _ = std::fs::remove_file(database_file_path.clone()); // ignore potential errors

    let inspector = DatabaseInspector::sqlite(database_file_path);
    let connectional = Arc::clone(&inspector.connectional);

    (Arc::new(inspector), connectional)
}

fn postgres() -> (Arc<DatabaseInspector>, Arc<Connectional>) {
    let url = format!("postgresql://postgres:prisma@127.0.0.1:5432/db?schema={}", SCHEMA);
    let inspector = DatabaseInspector::postgres(url.to_string());
    let connectional = Arc::clone(&inspector.connectional);

    (Arc::new(inspector), connectional)
}