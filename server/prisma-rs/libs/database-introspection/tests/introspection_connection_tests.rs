#![allow(non_snake_case)]
#![allow(unused)]

use barrel::{types, Migration};
use database_introspection::*;
use log::debug;
use prisma_query::connector::{Queryable, Sqlite as SqliteDatabaseClient};
use std::collections::HashSet;
use std::path::Path;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use std::{thread, time};

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

fn binary_array_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "BINARY[]".to_string(),
        "postgres" => "_bytea".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn bool_array_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "BOOLEAN[]".to_string(),
        "postgres" => "_bool".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn datetime_array_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "DATE[]".to_string(),
        "postgres" => "_date".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn double_array_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "DOUBLE[]".to_string(),
        "postgres" => "_float8".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn int_array_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "INTEGER[]".to_string(),
        "postgres" => "_int4".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn text_array_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "TEXT[]".to_string(),
        "postgres" => "_text".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn binary_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "BINARY".to_string(),
        "postgres" => "bytea".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn bool_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "BOOLEAN".to_string(),
        "postgres" => "bool".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn datetime_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "DATE".to_string(),
        "postgres" => "date".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn double_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "DOUBLE".to_string(),
        "postgres" => "float8".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn float_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "REAL".to_string(),
        "postgres" => "float8".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn int_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "INTEGER".to_string(),
        "postgres" => "int4".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn text_type(db_type: &str) -> String {
    match db_type {
        "sqlite" => "TEXT".to_string(),
        "postgres" => "text".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
}

fn varchar_type(db_type: &str, length: u64) -> String {
    match db_type {
        "sqlite" => format!("VARCHAR({})", length),
        "postgres" => "varchar".to_string(),
        _ => panic!(format!("unrecognized database type {}", db_type)),
    }
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
                // TODO: Test also autoincrement variety
                t.add_column("primary_col", types::primary());
                t.add_column("string1_col", types::text());
                t.add_column("string2_col", types::varchar(1));
                if db_type != "sqlite" {
                    t.add_column("json_col", types::json());
                    t.add_column("uuid_col", types::uuid());
                }
            });
        },
        |db_type, inspector| {
            let result = inspector.introspect(&SCHEMA.to_string()).expect("introspection");
            let table = result.get_table("User").expect("couldn't get User table");
            let mut expected_columns = vec![
                Column {
                    name: "array_bin_col".to_string(),
                    tpe: ColumnType {
                        raw: binary_array_type(db_type),
                        family: ColumnTypeFamily::Binary,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_bool_col".to_string(),
                    tpe: ColumnType {
                        raw: bool_array_type(db_type),
                        family: ColumnTypeFamily::Boolean,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_date_col".to_string(),
                    tpe: ColumnType {
                        raw: datetime_array_type(db_type),
                        family: ColumnTypeFamily::DateTime,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_double_col".to_string(),
                    tpe: ColumnType {
                        raw: double_array_type(db_type),
                        family: ColumnTypeFamily::Float,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_int_col".to_string(),
                    tpe: ColumnType {
                        raw: int_array_type(db_type),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "array_text_col".to_string(),
                    tpe: ColumnType {
                        raw: text_array_type(db_type),
                        family: ColumnTypeFamily::String,
                    },
                    arity: ColumnArity::List,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "binary_col".to_string(),
                    tpe: ColumnType {
                        raw: binary_type(db_type),
                        family: ColumnTypeFamily::Binary,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "boolean_col".to_string(),
                    tpe: ColumnType {
                        raw: bool_type(db_type),
                        family: ColumnTypeFamily::Boolean,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "date_time_col".to_string(),
                    tpe: ColumnType {
                        raw: datetime_type(db_type),
                        family: ColumnTypeFamily::DateTime,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "double_col".to_string(),
                    tpe: ColumnType {
                        raw: double_type(db_type),
                        family: ColumnTypeFamily::Float,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "float_col".to_string(),
                    tpe: ColumnType {
                        raw: float_type(db_type),
                        family: ColumnTypeFamily::Float,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "int_col".to_string(),
                    tpe: ColumnType {
                        raw: int_type(db_type),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "primary_col".to_string(),
                    tpe: ColumnType {
                        raw: int_type(db_type),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: match db_type {
                        "postgres" => Some(format!(
                            "nextval(\'\"{}\".\"User_primary_col_seq\"\'::regclass)",
                            SCHEMA
                        )),
                        _ => None,
                    },
                    auto_increment: None,
                },
                Column {
                    name: "string1_col".to_string(),
                    tpe: ColumnType {
                        raw: text_type(db_type),
                        family: ColumnTypeFamily::String,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "string2_col".to_string(),
                    tpe: ColumnType {
                        raw: varchar_type(db_type, 1),
                        family: ColumnTypeFamily::String,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
            ];
            if db_type != "sqlite" {
                expected_columns.push(Column {
                    name: "json_col".to_string(),
                    tpe: ColumnType {
                        raw: "json".to_string(),
                        family: ColumnTypeFamily::Json,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                });
                expected_columns.push(Column {
                    name: "uuid_col".to_string(),
                    tpe: ColumnType {
                        raw: "uuid".to_string(),
                        family: ColumnTypeFamily::Uuid,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                });
            }
            expected_columns.sort_unstable_by_key(|c| c.name.to_owned());

            assert_eq!(
                table,
                &Table {
                    name: "User".to_string(),
                    columns: expected_columns,
                    indices: vec![],
                    primary_key: Some(PrimaryKey {
                        columns: vec!["primary_col".to_string()],
                    }),
                    foreign_keys: vec![],
                }
            );
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

            let user_table = result.get_table("User").expect("getting User table");
            let expected_columns = vec![
                Column {
                    name: "column1".to_string(),
                    tpe: ColumnType {
                        raw: int_type(db_type),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: None,
                },
                Column {
                    name: "column2".to_string(),
                    tpe: ColumnType {
                        raw: int_type(db_type),
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
                if db_type == "mysql" {
                    t.add_column("city", types::integer());
                    t.inject_custom("FOREIGN KEY(city) REFERENCES City(id)");
                } else {
                    t.add_column("city", types::foreign("City", "id"));
                }
            });
        },
        |db_type, inspector| {
            let schema = inspector.introspect(&SCHEMA.to_string()).expect("introspection");
            let user_table = schema.get_table("User").expect("couldn't get User table");
            let expected_columns = vec![Column {
                name: "city".to_string(),
                tpe: ColumnType {
                    raw: int_type(db_type),
                    family: ColumnTypeFamily::Int,
                },
                arity: ColumnArity::Required,
                default: None,
                auto_increment: None,
            }];

            assert_eq!(
                user_table,
                &Table {
                    name: "User".to_string(),
                    columns: expected_columns,
                    indices: vec![],
                    primary_key: None,
                    foreign_keys: vec![ForeignKey {
                        columns: vec!["city".to_string()],
                        referenced_columns: vec!["id".to_string()],
                        referenced_table: "City".to_string(),
                    }],
                }
            );
        },
    );
}

#[test]
fn postgres_enums_must_work() {
    setup();

    let mut inspector = get_postgres_connector(&format!(
        "CREATE TYPE \"{}\".\"mood\" AS ENUM ('sad', 'ok', 'happy')",
        SCHEMA
    ));

    let schema = inspector.introspect(&SCHEMA.to_string()).expect("introspection");
    let got_enum = schema.get_enum("mood").expect("get enum");

    let values: HashSet<String> = ["happy".to_string(), "ok".to_string(), "sad".to_string()]
        .iter()
        .cloned()
        .collect();
    assert_eq!(
        got_enum,
        &Enum {
            name: "mood".to_string(),
            values,
        }
    );
}

#[test]
fn postgres_sequences_must_work() {
    setup();

    let mut inspector = get_postgres_connector(&format!("CREATE SEQUENCE \"{}\".\"test\"", SCHEMA));

    let schema = inspector.introspect(&SCHEMA.to_string()).expect("introspection");
    let got_seq = schema.get_sequence("test").expect("get sequence");

    assert_eq!(
        got_seq,
        &Sequence {
            name: "test".to_string(),
            initial_value: 1,
            allocation_size: 1,
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
        let full_sql = migration.make::<barrel::backend::Sqlite>();
        let mut inspector = get_sqlite_connector(&full_sql);

        testFn("sqlite", &mut inspector);
    }
    // POSTGRES
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn("postgres", &mut migration);
        let full_sql = migration.make::<barrel::backend::Pg>();
        let mut inspector = get_postgres_connector(&full_sql);

        testFn("postgres", &mut inspector);
    }
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

struct SqliteConnection {
    client: Mutex<prisma_query::connector::Sqlite>,
}

impl crate::IntrospectionConnection for SqliteConnection {
    fn query_raw(&self, sql: &str, schema: &str) -> prisma_query::Result<prisma_query::connector::ResultSet> {
        self.client.lock().expect("self.client.lock").query_raw(sql, &[])
    }
}

fn get_sqlite_connector(sql: &str) -> sqlite::IntrospectionConnector {
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let database_folder_path = format!("{}/db", server_root);
    let database_file_path = dbg!(format!("{}/{}.db", database_folder_path, SCHEMA));
    if Path::new(&database_file_path).exists() {
        std::fs::remove_file(database_file_path.clone()).expect("remove database file");
    }

    let conn = rusqlite::Connection::open_in_memory().expect("opening SQLite connection should work");
    conn.execute(
        "ATTACH DATABASE ? as ?",
        &vec![database_file_path.clone(), String::from(SCHEMA)],
    )
    .expect("attach SQLite database");
    conn.execute_batch(sql).expect("executing migration");
    conn.close().expect("closing SQLite connection");

    let mut queryable =
        prisma_query::connector::Sqlite::new(database_file_path).expect("opening prisma_query::connector::Sqlite");
    queryable.attach_database(SCHEMA).expect("attaching database");
    let int_conn = Arc::new(SqliteConnection {
        client: Mutex::new(queryable),
    });
    sqlite::IntrospectionConnector::new(int_conn)
}

struct PostgresConnection {
    client: Mutex<prisma_query::connector::PostgreSql>,
}

impl crate::IntrospectionConnection for PostgresConnection {
    fn query_raw(&self, sql: &str, schema: &str) -> prisma_query::Result<prisma_query::connector::ResultSet> {
        self.client.lock().expect("self.client.lock").query_raw(sql, &[])
    }
}

fn get_postgres_connector(sql: &str) -> postgres::IntrospectionConnector {
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

    client
        .execute(format!("CREATE SCHEMA \"{}\";", SCHEMA).as_str(), &[])
        .expect("creating schema");

    let sql_string = sql.to_string();
    let statements: Vec<&str> = sql_string.split(";").collect();
    for statement in statements {
        debug!("Executing migration statement: '{}'", statement);
        client.execute(statement, &[]).expect("executing migration statement");
    }

    let conn = Arc::new(PostgresConnection {
        client: Mutex::new(prisma_query::connector::PostgreSql::from(client)),
    });
    postgres::IntrospectionConnector::new(conn)
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
