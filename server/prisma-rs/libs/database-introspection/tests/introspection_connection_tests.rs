#![allow(non_snake_case)]
#![allow(unused)]

use barrel::{types, Migration};
use database_introspection::*;
use prisma_query::connector::{Queryable, Sqlite as SqliteDatabaseClient};
use std::sync::Arc;
use std::{thread, time};
use std::sync::atomic::{AtomicBool, Ordering};
use std::path::Path;

const SCHEMA: &str = "DatabaseInspectorTest";

static IS_SETUP: AtomicBool = AtomicBool::new(false);

fn setup() {
    let is_setup = IS_SETUP.load(Ordering::Relaxed);

    if is_setup {
        return;
    }

    fern::Dispatch::new()
        .format(|out, message, record| {
            out.finish(format_args!("[{}][{}] {}", record.target(), record.level(), message))
        })
        .level(log::LevelFilter::Warn)
        .chain(std::io::stdout())
        .apply()
        .expect("fern configuration");

    IS_SETUP.store(true, Ordering::Relaxed);
}

#[test]
fn all_column_types_must_work() {
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
        |db_type, inspector| {
            let result = inspector.introspect(&SCHEMA.to_string()).expect("introspection");
            let table = result.table("User").expect("couldn't get User table");
            let expected_columns = vec![
                Column {
                    name: "array_bin_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("BINARY[]"),
                        family: ColumnTypeFamily::Binary,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_bool_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("BOOLEAN[]"),
                        family: ColumnTypeFamily::Boolean,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_date_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("DATE[]"),
                        family: ColumnTypeFamily::DateTime,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_double_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("DOUBLE[]"),
                        family: ColumnTypeFamily::Float,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_int_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("INTEGER[]"),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_text_col".to_string(),
                    tpe: ColumnType {
                        raw: String::from("TEXT[]"),
                        family: ColumnTypeFamily::String,
                    },
                    arity: ColumnArity::List,
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
                        family: ColumnTypeFamily::Float,
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
        |db_type, inspector| {
            let result = inspector.introspect(&SCHEMA.to_string()).expect("introspecting");

            let user_table = result.table("User").expect("getting User table");
            let expected_columns = vec![
                Column {
                    name: "column1".to_string(),
                    tpe: ColumnType {
                        raw: match db_type {
                            "sqlite" => "INTEGER".to_string(),
                            "postgres" => "int4".to_string(),
                            "mysql" => "int4".to_string(),
                            _ => panic!(format!("unrecognized database type {}", db_type)),
                        },
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "column2".to_string(),
                    tpe: ColumnType {
                        raw: match db_type {
                            "sqlite" => "INTEGER".to_string(),
                            "postgres" => "int4".to_string(),
                            "mysql" => "int4".to_string(),
                            _ => panic!(format!("unrecognized database type {}", db_type)),
                        },
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

#[test]
fn foreign_keys_must_work() {
    setup();

    test_each_backend(
        |db_type, mut migration| {
            let db_type = db_type.clone();
            migration.create_table("City", |t| {
                t.add_column("id", types::primary());
            });
            migration.create_table("User", move |t| {
                // barrel does not render foreign keys correctly for mysql
                // TODO: Investigate
                if db_type == "mysql"{
                    t.add_column("city", types::integer());
                    t.inject_custom("FOREIGN KEY(city) REFERENCES City(id)");
                } else {
                    t.add_column("city", types::foreign("City", "id"));
                }
            });
        },
        |db_type, inspector| {
            let schema = inspector.introspect(&SCHEMA.to_string()).expect("introspection");
            let user_table = schema.table("User").expect("couldn't get User table");
            let expected_columns = vec![Column {
                name: "city".to_string(),
                tpe: ColumnType{
                    raw: "INTEGER".to_string(),
                    family: ColumnTypeFamily::Int,
                },
                arity: ColumnArity::Required,
                default: None,
                auto_increment: None,
            }];

            assert_eq!(user_table, &Table{
                name: "User".to_string(),
                columns: expected_columns,
                indexes: vec![],
                primary_key: None,
                foreign_keys: vec![ForeignKey{
                    column: "city".to_string(),
                    referenced_column: "id".to_string(),
                    referenced_table: "City".to_string(),
                }],
            });
        },
    );
}

fn test_each_backend<MigrationFn, TestFn>(mut migrationFn: MigrationFn, testFn: TestFn)
where
    MigrationFn: FnMut(&'static str, &mut Migration) -> (),
    TestFn: Fn(&'static str, &mut IntrospectionConnector) -> (),
{
    // SQLITE
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn("sqlite", &mut migration);
        let mut inspector = get_sqlite_connector(migration);

        testFn("sqlite", &mut inspector);
    }
    // POSTGRES
    // {
    //     let mut migration = Migration::new().schema(SCHEMA);
    //     migrationFn("postgres", &mut migration);
    //     let mut inspector = get_postgres_connector(migration);
    //     // let full_sql = migration.make::<barrel::backend::Pg>();
    //     // run_full_sql(&queryable, &full_sql);

    //     testFn("postgres", &mut inspector);
    // }
    // // MySQL
    // {
    //     let mut migration = Migration::new().schema(SCHEMA);
    //     migrationFn("mysql", &mut migration);
    //     let (inspector, queryable) = mysql();
    //     let full_sql = dbg!(migration.make::<barrel::backend::MySql>());
    //     run_full_sql(&queryable, &full_sql);
    //     testFn("mysql", inspector);
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

fn get_sqlite_connector(migration: Migration) -> sqlite::IntrospectionConnector {
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let database_folder_path = format!("{}/db", server_root);
    let database_file_path = dbg!(format!("{}/{}.db", database_folder_path, SCHEMA));
    if Path::new(&database_file_path).exists() {
        std::fs::remove_file(database_file_path.clone()).expect("remove database file");
    }

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

fn get_postgres_connector(migration: Migration) -> postgres::IntrospectionConnector {
    // Drop schema if it exists
    let host = match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db-postgres",
        Err(_) => "127.0.0.1",
    };
    let mut client = ::postgres::Config::new()
        .user("prisma")
        .password("prisma")
        .host(host)
        .port(5432)
        .dbname("prisma-test")
        .connect(::postgres::NoTls)
        .expect("connecting to Postgres");

    let drop_schema = format!("DROP SCHEMA IF EXISTS \"{}\" CASCADE;", SCHEMA);
    client.execute(drop_schema.as_str(), &[]).expect("dropping schema");

    client.execute(format!("CREATE SCHEMA \"{}\";", SCHEMA).as_str(), &[]).expect("creating schema");

    let full_sql = migration.make::<barrel::backend::Pg>();
    client.execute(full_sql.as_str(), &[]).expect("executing migration");

    postgres::IntrospectionConnector::new(client).expect("creating Postgres connector")
}

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

fn db_host_mysql() -> String {
    match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db-mysql".to_string(),
        Err(_) => "127.0.0.1".to_string(),
    }
}
