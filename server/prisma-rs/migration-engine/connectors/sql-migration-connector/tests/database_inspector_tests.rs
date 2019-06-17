#![allow(non_snake_case)]
#![allow(unused)]

use barrel::{types, Migration};
use sql_migration_connector::database_inspector::*;
use prisma_query::connector::Sqlite as SqliteDatabaseClient;
use prisma_query::{Connectional, ResultSet};
use std::sync::Arc;
use std::{thread, time};

const SCHEMA: &str = "DatabaseInspectorTest";

#[test]
fn all_columns_types_must_work() {
    test_each_backend(
        |_, mut migration| {
            migration.create_table("User", |t| {
                t.add_column("int_col", types::integer());
                t.add_column("float_col", types::float());
                t.add_column("boolean_col", types::boolean());
                t.add_column("string1_col", types::text());
                t.add_column("string2_col", types::varchar(1));
                t.add_column("date_time_col", types::date());
            });
        },
        |inspector| {
            let result = inspector.introspect(&SCHEMA.to_string());

            let table = result.table("User").unwrap();
            let expected_columns = vec![
                Column {
                    name: "int_col".to_string(),
                    tpe: ColumnType::Int,
                    is_required: true,
                    foreign_key: None,
                    sequence: None,
                    default: None,
                },
                Column {
                    name: "float_col".to_string(),
                    tpe: ColumnType::Float,
                    is_required: true,
                    foreign_key: None,
                    sequence: None,
                    default: None,
                },
                Column {
                    name: "boolean_col".to_string(),
                    tpe: ColumnType::Boolean,
                    is_required: true,
                    foreign_key: None,
                    sequence: None,
                    default: None,
                },
                Column {
                    name: "string1_col".to_string(),
                    tpe: ColumnType::String,
                    is_required: true,
                    foreign_key: None,
                    sequence: None,
                    default: None,
                },
                Column {
                    name: "string2_col".to_string(),
                    tpe: ColumnType::String,
                    is_required: true,
                    foreign_key: None,
                    sequence: None,
                    default: None,
                },
                Column {
                    name: "date_time_col".to_string(),
                    tpe: ColumnType::DateTime,
                    is_required: true,
                    foreign_key: None,
                    sequence: None,
                    default: None,
                },
            ];

            assert_eq!(table.columns, expected_columns);
        },
    );
}

#[test]
fn is_required_must_work() {
    test_each_backend(
        |_, mut migration| {
            migration.create_table("User", |t| {
                t.add_column("column1", types::integer().nullable(false));
                t.add_column("column2", types::integer().nullable(true));
            });
        },
        |inspector| {
            let result = inspector.introspect(&SCHEMA.to_string());

            let user_table = result.table("User").unwrap();
            let expected_columns = vec![
                Column {
                    name: "column1".to_string(),
                    tpe: ColumnType::Int,
                    is_required: true,
                    foreign_key: None,
                    sequence: None,
                    default: None,
                },
                Column {
                    name: "column2".to_string(),
                    tpe: ColumnType::Int,
                    is_required: false,
                    foreign_key: None,
                    sequence: None,
                    default: None,
                },
            ];
            assert_eq!(user_table.columns, expected_columns);
        },
    );
}

#[test]
fn foreign_keys_must_work() {
    test_each_backend(
        |db_type, mut migration| {
            let db_type = db_type.clone();
            migration.create_table("City", |t| {
                t.add_column("id", types::primary());
            });
            migration.create_table("User", move |t| {
                // barrel does not render foreign keys correctly for mysql
                if db_type == "mysql"{
                    t.add_column("city", types::integer());
                    t.inject_custom("FOREIGN KEY(city) REFERENCES City(id)");
                } else {
                    t.add_column("city", types::foreign("City", "id")); 
                }
            });
        },
        |inspector| {
            let result = inspector.introspect(&SCHEMA.to_string());

            let user_table = result.table("User").unwrap();
            let expected_columns = vec![Column {
                name: "city".to_string(),
                tpe: ColumnType::Int,
                is_required: true,
                foreign_key: Some(ForeignKey::new("City".to_string(), "id".to_string())),
                sequence: None,
                default: None,
            }];
            assert_eq!(user_table.columns, expected_columns);
        },
    );
}

fn test_each_backend<MigrationFn, TestFn>(mut migrationFn: MigrationFn, testFn: TestFn)
where
    MigrationFn: FnMut(&'static str, &mut Migration) -> (),
    TestFn: Fn(Arc<DatabaseInspector>) -> (),
{

    println!("Testing with SQLite now");
    // SQLITE
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn("sqlite", &mut migration);
        let (inspector, connectional) = sqlite();
        let full_sql = migration.make::<barrel::backend::Sqlite>();
        run_full_sql(&connectional, &full_sql);
        println!("Running the test function now");
        testFn(inspector);
    }
    println!("Testing with Postgres now");
    // POSTGRES
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn("postgres", &mut migration);
        let (inspector, connectional) = postgres();
        let full_sql = migration.make::<barrel::backend::Pg>();
        run_full_sql(&connectional, &full_sql);
        println!("Running the test function now");
        testFn(inspector);
    }
    println!("Testing with MySQL now");
    // MySQL
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn("mysql", &mut migration);
        let (inspector, connectional) = mysql();
        let full_sql = dbg!(migration.make::<barrel::backend::MySql>());
        run_full_sql(&connectional, &full_sql);
        println!("Running the test function now");
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
    let drop_schema = dbg!(format!("DROP SCHEMA IF EXISTS \"{}\" CASCADE;", SCHEMA));
    let setup_connectional = DatabaseInspector::postgres(url.to_string()).connectional;
    let _ = setup_connectional.query_on_raw_connection(&SCHEMA, &drop_schema, &[]);

    let inspector = DatabaseInspector::postgres(url.to_string());
    let connectional = Arc::clone(&inspector.connectional);

    (Arc::new(inspector), connectional)
}

fn mysql() -> (Arc<DatabaseInspector>, Arc<Connectional>) {
    let url_without_db = "mysql://root:prisma@127.0.0.1:3306";
    let drop_database = dbg!(format!("DROP DATABASE IF EXISTS `{}`;", SCHEMA));
    let create_database = dbg!(format!("CREATE DATABASE `{}`;", SCHEMA));
    let setup_connectional = DatabaseInspector::mysql(url_without_db.to_string()).connectional;
    let _ = setup_connectional.query_on_raw_connection(&SCHEMA, &drop_database, &[]);
    let _ = setup_connectional.query_on_raw_connection(&SCHEMA, &create_database, &[]);

    let url = format!("mysql://root:prisma@127.0.0.1:3306/{}", SCHEMA);
    let inspector = DatabaseInspector::mysql(url.to_string());
    let connectional = Arc::clone(&inspector.connectional);

    (Arc::new(inspector), connectional)
}

fn string_to_static_str(s: String) -> &'static str {
    Box::leak(s.into_boxed_str())
}
