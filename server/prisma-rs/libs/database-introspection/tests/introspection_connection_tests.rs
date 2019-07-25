#![allow(non_snake_case)]
#![allow(unused)]

use barrel::{types, Migration};
use database_introspection::*;
use prisma_query::connector::{Queryable, Sqlite as SqliteDatabaseClient};
use std::sync::Arc;
use std::{thread, time};

const SCHEMA: &str = "DatabaseInspectorTest";

fn setup() {
    fern::Dispatch::new()
        .format(|out, message, record| {
            out.finish(format_args!("[{}][{}] {}", record.target(), record.level(), message))
        })
        .level(log::LevelFilter::Debug)
        .chain(std::io::stdout())
        .apply()
        .expect("fern configuration");
}

#[test]
fn all_columns_types_must_work() {
    setup();

    test_each_backend(
        |db_type, mut migration| {
            migration.create_table("User", move |t| {
                t.add_column("array_bin_col", types::array(&types::binary()));
                t.add_column("array_bool_col", types::array(&types::boolean()));
                t.add_column("array_date_col", types::array(&types::date()));
                t.add_column("array_double_col", types::array(&types::double()));
                // TODO: Figure out
                // t.add_column("array_float_col", types::array(&types::float()));
                t.add_column("array_int_col", types::array(&types::integer()));
                t.add_column("array_text_col", types::array(&types::text()));
                // TODO: Figure out
                // t.add_column("array_varchar_col", types::array(&types::varchar(255)));
                t.add_column("binary_col", types::binary());
                t.add_column("boolean_col", types::boolean());
                t.add_column("date_time_col", types::date());
                t.add_column("double_col", types::double());
                t.add_column("float_col", types::float());
                t.add_column("int_col", types::integer());
                if db_type != "sqlite" {
                    t.add_column("json_col", types::json());
                }
                // TODO: Test also autoincrement variety
                t.add_column("primary_col", types::primary());
                t.add_column("string1_col", types::text());
                if db_type != "sqlite" {
                    t.add_column("uuid_col", types::uuid());
                }
                t.add_column("string2_col", types::varchar(1));
            });
        },
        |inspector| {
            let result = inspector.introspect(&SCHEMA.to_string()).unwrap();

            let table = result.table("User").unwrap();
            let expected_columns = vec![
                Column {
                    name: "array_bin_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("BINARY[]"),
                        family: ColumnTypeFamily::BinArray,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_bool_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("BOOLEAN[]"),
                        family: ColumnTypeFamily::BoolArray,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_date_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("DATE[]"),
                        family: ColumnTypeFamily::DateTimeArray,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_double_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("DOUBLE[]"),
                        family: ColumnTypeFamily::DoubleArray,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_int_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("INTEGER[]"),
                        family: ColumnTypeFamily::IntArray,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_text_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("TEXT[]"),
                        family: ColumnTypeFamily::StringArray,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "binary_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("BINARY"),
                        family: ColumnTypeFamily::Binary,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "boolean_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("BOOLEAN"),
                        family: ColumnTypeFamily::Boolean,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "date_time_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("DATE"),
                        family: ColumnTypeFamily::DateTime,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "double_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("DOUBLE"),
                        family: ColumnTypeFamily::Double,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "float_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("REAL"),
                        family: ColumnTypeFamily::Float,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "int_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("INTEGER"),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "primary_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("INTEGER"),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "string1_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("TEXT"),
                        family: ColumnTypeFamily::String,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "string2_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("VARCHAR(1)"),
                        family: ColumnTypeFamily::String,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
            ];

            assert_eq!(table.name, "User");
            assert_eq!(table.columns, expected_columns);
            assert_eq!(table.indexes, vec![]);
            assert_eq!(
                table.primary_key,
                Some(PrimaryKey {
                    columns: vec!["primary_col".to_string()],
                })
            );
            assert_eq!(table.foreign_keys, vec![]);
        },
    );
}

#[test]
fn is_required_must_work() {
    setup();

    test_each_backend(
        |_, mut migration| {
            migration.create_table("User", |t| {
                t.add_column("column1", types::integer().nullable(false));
                t.add_column("column2", types::integer().nullable(true));
            });
        },
        |inspector| {
            let result = inspector.introspect(&SCHEMA.to_string()).unwrap();

            let user_table = result.table("User").unwrap();
            let expected_columns = vec![
                Column {
                    name: "column1".to_string(),
                    tpe: ColumnType {
                        raw: String::from("INTEGER"),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "column2".to_string(),
                    tpe: ColumnType {
                        raw: String::from("INTEGER"),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Nullable,
                    default: None,
                    auto_increment: None,
                },
            ];
            assert_eq!(user_table.columns, expected_columns);
        },
    );
}

// #[test]
// fn foreign_keys_must_work() {
//     test_each_backend(
//         |db_type, mut migration| {
//             let db_type = db_type.clone();
//             migration.create_table("City", |t| {
//                 t.add_column("id", types::primary());
//             });
//             migration.create_table("User", move |t| {
//                 // barrel does not render foreign keys correctly for mysql
//                 if db_type == "mysql"{
//                     t.add_column("city", types::integer());
//                     t.inject_custom("FOREIGN KEY(city) REFERENCES City(id)");
//                 } else {
//                     t.add_column("city", types::foreign("City", "id"));
//                 }
//             });
//         },
//         |inspector| {
//             let result = inspector.introspect(&SCHEMA.to_string());

//             let user_table = result.table("User").unwrap();
//             let expected_columns = vec![Column {
//                 name: "city".to_string(),
//                 tpe: ColumnType::Int,
//                 is_required: true,
//                 foreign_key: Some(ForeignKey::new("City".to_string(), "id".to_string())),
//                 sequence: None,
//                 default: None,
//             }];
//             assert_eq!(user_table.columns, expected_columns);
//         },
//     );
// }

fn test_each_backend<MigrationFn, TestFn>(mut migrationFn: MigrationFn, testFn: TestFn)
where
    MigrationFn: FnMut(&'static str, &mut Migration) -> (),
    TestFn: Fn(&mut IntrospectionConnector) -> (),
{
    println!("Testing with SQLite now");
    // SQLITE
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn("sqlite", &mut migration);
        let mut inspector = sqlite(migration);
        testFn(&mut inspector);
    }
    // println!("Testing with Postgres now");
    // // POSTGRES
    // {
    //     let mut migration = Migration::new().schema(SCHEMA);
    //     migrationFn("postgres", &mut migration);
    //     let (inspector, queryable) = postgres();
    //     let full_sql = migration.make::<barrel::backend::Pg>();
    //     run_full_sql(&queryable, &full_sql);
    //     println!("Running the test function now");
    //     testFn(inspector);
    // }
    // println!("Testing with MySQL now");
    // // MySQL
    // {
    //     let mut migration = Migration::new().schema(SCHEMA);
    //     migrationFn("mysql", &mut migration);
    //     let (inspector, queryable) = mysql();
    //     let full_sql = dbg!(migration.make::<barrel::backend::MySql>());
    //     run_full_sql(&queryable, &full_sql);
    //     println!("Running the test function now");
    //     testFn(inspector);
    // }
}

fn run_full_sql(queryable: &mut Queryable, full_sql: &str) {
    for sql in full_sql.split(";") {
        dbg!(sql);
        if sql != "" {
            queryable.query_raw(&sql, &[]).expect("executing SQL should work");
        }
    }
}

fn sqlite(migration: Migration) -> sqlite::IntrospectionConnector {
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let database_folder_path = format!("{}/db", server_root);
    let database_file_path = dbg!(format!("{}/{}.db", database_folder_path, SCHEMA));
    std::fs::remove_file(database_file_path.clone()).expect("remove database file");

    let full_sql = migration.make::<barrel::backend::Sqlite>();
    let conn = rusqlite::Connection::open_in_memory().expect("opening SQLite connection should work");
    conn.execute(
        "ATTACH DATABASE ? as ?",
        &vec![database_file_path.clone(), String::from(SCHEMA)],
    )
    .expect("attach SQLite database");
    conn.execute_batch(&full_sql).expect("executing migration");
    conn.close().expect("closing SQLite connection");

    sqlite::IntrospectionConnector::new(&database_file_path, SCHEMA).expect("creating SQLite connector should work")
}

// fn postgres() -> (Arc<IntrospectionConnector>, Arc<Connectional>) {
//     let url = format!("postgresql://postgres:prisma@{}:5432/db?schema={}", db_host_postgres(), SCHEMA);
//     let drop_schema = dbg!(format!("DROP SCHEMA IF EXISTS \"{}\" CASCADE;", SCHEMA));
//     let setup_connectional = DatabaseInspector::postgres(url.to_string()).queryable;
//     let _ = setup_connectional.query_on_raw_connection(&SCHEMA, &drop_schema, &[]);

//     let inspector = DatabaseInspector::postgres(url.to_string());
//     let queryable = Arc::clone(&inspector.queryable);

//     (Arc::new(inspector), queryable)
// }

// fn mysql() -> (Arc<IntrospectionConnector>, Arc<Connectional>) {
//     let url_without_db = format!("mysql://root:prisma@{}:3306", db_host_mysql());
//     let drop_database = dbg!(format!("DROP DATABASE IF EXISTS `{}`;", SCHEMA));
//     let create_database = dbg!(format!("CREATE DATABASE `{}`;", SCHEMA));
//     let setup_connectional = DatabaseInspector::mysql(url_without_db.to_string()).queryable;
//     let _ = setup_connectional.query_on_raw_connection(&SCHEMA, &drop_database, &[]);
//     let _ = setup_connectional.query_on_raw_connection(&SCHEMA, &create_database, &[]);

//     let url = format!("mysql://root:prisma@{}:3306/{}", db_host_mysql(),  SCHEMA);
//     let inspector = DatabaseInspector::mysql(url.to_string());
//     let queryable = Arc::clone(&inspector.queryable);

//     (Arc::new(inspector), queryable)
// }

fn db_host_postgres() -> String {
    match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db-postgres".to_string(),
        Err(_) => "127.0.0.1".to_string(),
    }
}

fn db_host_mysql() -> String {
    match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db-mysql".to_string(),
        Err(_) => "127.0.0.1".to_string(),
    }
}
