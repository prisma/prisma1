#![allow(non_snake_case)]
#![allow(unused)]

use barrel::{types, Migration};
use database_introspection::*;
use log::{debug, LevelFilter};
use pretty_assertions::assert_eq;
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

    let log_level = match std::env::var("RUST_LOG")
        .unwrap_or("warn".to_string())
        .to_lowercase()
        .as_ref()
    {
        "trace" => LevelFilter::Trace,
        "debug" => LevelFilter::Debug,
        "info" => LevelFilter::Info,
        "warn" => LevelFilter::Warn,
        "error" => LevelFilter::Error,
        _ => LevelFilter::Warn,
    };
    fern::Dispatch::new()
        .format(|out, message, record| {
            out.finish(format_args!("[{}][{}] {}", record.target(), record.level(), message))
        })
        .level(log_level)
        .chain(std::io::stdout())
        .apply()
        .expect("fern configuration");

    IS_SETUP.store(true, Ordering::Relaxed);
}

#[derive(Debug, PartialEq, Copy, Clone)]
enum DbType {
    Postgres,
    MySql,
    Sqlite,
}

fn binary_array_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "_bytea".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn bool_array_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "_bool".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn datetime_array_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "_date".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn double_array_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "_float8".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn float_array_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "_float8".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn int_array_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "_int4".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn text_array_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "_text".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn varchar_array_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "_varchar".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn binary_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "bytea".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn bool_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "bool".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn datetime_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "date".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn double_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "float8".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn float_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "float8".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn int_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "int4".to_string(),
        DbType::Sqlite => "INTEGER".to_string(),
        DbType::MySql => "int".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn text_type(db_type: DbType) -> String {
    match db_type {
        DbType::Postgres => "text".to_string(),
        DbType::Sqlite => "TEXT".to_string(),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

fn varchar_type(db_type: DbType, length: u64) -> String {
    match db_type {
        DbType::Postgres => "varchar".to_string(),
        DbType::MySql => "varchar".to_string(),
        DbType::Sqlite => format!("VARCHAR({})", length),
        _ => panic!(format!("unrecognized database type {:?}", db_type)),
    }
}

#[test]
fn all_postgres_column_types_must_work() {
    setup();

    let mut migration = Migration::new().schema(SCHEMA);
    migration.create_table("User", move |t| {
        t.add_column("array_bin_col", types::array(&types::binary()));
        t.add_column("array_bool_col", types::array(&types::boolean()));
        t.add_column("array_date_col", types::array(&types::date()));
        t.add_column("array_double_col", types::array(&types::double()));
        t.add_column("array_float_col", types::array(&types::float()));
        t.add_column("array_int_col", types::array(&types::integer()));
        t.add_column("array_text_col", types::array(&types::text()));
        t.add_column("array_varchar_col", types::array(&types::varchar(255)));
        t.add_column("bigint_col", types::custom("BIGINT"));
        t.add_column("bigserial_col", types::custom("BIGSERIAL"));
        t.add_column("bit_col", types::custom("BIT"));
        t.add_column("bit_varying_col", types::custom("BIT VARYING(1)"));
        t.add_column("binary_col", types::binary());
        t.add_column("boolean_col", types::boolean());
        t.add_column("box_col", types::custom("BOX"));
        t.add_column("char_col", types::custom("CHARACTER(1)"));
        t.add_column("circle_col", types::custom("CIRCLE"));
        t.add_column("date_time_col", types::date());
        t.add_column("double_col", types::double());
        t.add_column("float_col", types::float());
        t.add_column("int_col", types::integer());
        t.add_column("interval_col", types::custom("INTERVAL"));
        t.add_column("line_col", types::custom("LINE"));
        t.add_column("lseg_col", types::custom("LSEG"));
        t.add_column("numeric_col", types::custom("NUMERIC"));
        t.add_column("path_col", types::custom("PATH"));
        t.add_column("pg_lsn_col", types::custom("PG_LSN"));
        t.add_column("polygon_col", types::custom("POLYGON"));
        t.add_column("smallint_col", types::custom("SMALLINT"));
        t.add_column("smallserial_col", types::custom("SMALLSERIAL"));
        t.add_column("serial_col", types::custom("SERIAL"));
        // TODO: Test also autoincrement variety
        t.add_column("primary_col", types::primary());
        t.add_column("string1_col", types::text());
        t.add_column("string2_col", types::varchar(1));
        t.add_column("time_col", types::custom("TIME"));
        t.add_column("time_with_zone_col", types::custom("TIME WITH TIME ZONE"));
        t.add_column("timestamp_col", types::custom("TIMESTAMP"));
        t.add_column("timestamp_with_zone_col", types::custom("TIMESTAMP WITH TIME ZONE"));
        t.add_column("tsquery_col", types::custom("TSQUERY"));
        t.add_column("tsvector_col", types::custom("TSVECTOR"));
        t.add_column("txid_col", types::custom("TXID_SNAPSHOT"));
        t.add_column("json_col", types::json());
        t.add_column("jsonb_col", types::custom("JSONB"));
        t.add_column("uuid_col", types::uuid());
    });

    let full_sql = migration.make::<barrel::backend::Pg>();
    let mut inspector = get_postgres_connector(&full_sql);
    let result = inspector.introspect(SCHEMA).expect("introspection");
    let mut table = result.get_table("User").expect("couldn't get User table").to_owned();
    // Ensure columns are sorted as expected when comparing
    table.columns.sort_unstable_by_key(|c| c.name.to_owned());
    let db_type = DbType::Postgres;
    let mut expected_columns = vec![
        Column {
            name: "array_bin_col".to_string(),
            tpe: ColumnType {
                raw: binary_array_type(db_type),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::List,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "array_bool_col".to_string(),
            tpe: ColumnType {
                raw: bool_array_type(db_type),
                family: ColumnTypeFamily::Boolean,
            },
            arity: ColumnArity::List,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "array_date_col".to_string(),
            tpe: ColumnType {
                raw: datetime_array_type(db_type),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::List,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "array_double_col".to_string(),
            tpe: ColumnType {
                raw: double_array_type(db_type),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::List,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "array_float_col".to_string(),
            tpe: ColumnType {
                raw: float_array_type(db_type),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::List,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "array_int_col".to_string(),
            tpe: ColumnType {
                raw: int_array_type(db_type),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::List,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "array_text_col".to_string(),
            tpe: ColumnType {
                raw: text_array_type(db_type),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::List,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "array_varchar_col".to_string(),
            tpe: ColumnType {
                raw: varchar_array_type(db_type),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::List,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "binary_col".to_string(),
            tpe: ColumnType {
                raw: binary_type(db_type),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "boolean_col".to_string(),
            tpe: ColumnType {
                raw: bool_type(db_type),
                family: ColumnTypeFamily::Boolean,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "date_time_col".to_string(),
            tpe: ColumnType {
                raw: datetime_type(db_type),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "double_col".to_string(),
            tpe: ColumnType {
                raw: double_type(db_type),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "float_col".to_string(),
            tpe: ColumnType {
                raw: float_type(db_type),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "int_col".to_string(),
            tpe: ColumnType {
                raw: int_type(db_type),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "primary_col".to_string(),
            tpe: ColumnType {
                raw: int_type(db_type),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: match db_type {
                DbType::Postgres => Some(format!(
                    "nextval(\'\"{}\".\"User_primary_col_seq\"\'::regclass)",
                    SCHEMA
                )),
                _ => None,
            },
            auto_increment: true,
        },
        Column {
            name: "string1_col".to_string(),
            tpe: ColumnType {
                raw: text_type(db_type),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "string2_col".to_string(),
            tpe: ColumnType {
                raw: varchar_type(db_type, 1),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "bigint_col".to_string(),
            tpe: ColumnType {
                raw: "int8".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "bigserial_col".to_string(),
            tpe: ColumnType {
                raw: "int8".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: Some(format!(
                "nextval(\'\"{}\".\"User_bigserial_col_seq\"\'::regclass)",
                SCHEMA
            )),
            auto_increment: true,
        },
        Column {
            name: "bit_col".to_string(),
            tpe: ColumnType {
                raw: "bit".to_string(),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "bit_varying_col".to_string(),
            tpe: ColumnType {
                raw: "varbit".to_string(),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "box_col".to_string(),
            tpe: ColumnType {
                raw: "box".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "char_col".to_string(),
            tpe: ColumnType {
                raw: "bpchar".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "circle_col".to_string(),
            tpe: ColumnType {
                raw: "circle".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "interval_col".to_string(),
            tpe: ColumnType {
                raw: "interval".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "line_col".to_string(),
            tpe: ColumnType {
                raw: "line".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "lseg_col".to_string(),
            tpe: ColumnType {
                raw: "lseg".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "numeric_col".to_string(),
            tpe: ColumnType {
                raw: "numeric".to_string(),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "path_col".to_string(),
            tpe: ColumnType {
                raw: "path".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "pg_lsn_col".to_string(),
            tpe: ColumnType {
                raw: "pg_lsn".to_string(),
                family: ColumnTypeFamily::LogSequenceNumber,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "polygon_col".to_string(),
            tpe: ColumnType {
                raw: "polygon".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "smallint_col".to_string(),
            tpe: ColumnType {
                raw: "int2".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "smallserial_col".to_string(),
            tpe: ColumnType {
                raw: "int2".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: Some(format!(
                "nextval('\"{}\".\"User_smallserial_col_seq\"'::regclass)",
                SCHEMA
            )),
            auto_increment: true,
        },
        Column {
            name: "serial_col".to_string(),
            tpe: ColumnType {
                raw: "int4".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: Some(format!("nextval('\"{}\".\"User_serial_col_seq\"'::regclass)", SCHEMA)),
            auto_increment: true,
        },
        Column {
            name: "time_col".to_string(),
            tpe: ColumnType {
                raw: "time".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "time_with_zone_col".to_string(),
            tpe: ColumnType {
                raw: "timetz".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "timestamp_col".to_string(),
            tpe: ColumnType {
                raw: "timestamp".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "timestamp_with_zone_col".to_string(),
            tpe: ColumnType {
                raw: "timestamptz".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "tsquery_col".to_string(),
            tpe: ColumnType {
                raw: "tsquery".to_string(),
                family: ColumnTypeFamily::TextSearch,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "tsvector_col".to_string(),
            tpe: ColumnType {
                raw: "tsvector".to_string(),
                family: ColumnTypeFamily::TextSearch,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "txid_col".to_string(),
            tpe: ColumnType {
                raw: "txid_snapshot".to_string(),
                family: ColumnTypeFamily::TransactionId,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "json_col".to_string(),
            tpe: ColumnType {
                raw: "json".to_string(),
                family: ColumnTypeFamily::Json,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "jsonb_col".to_string(),
            tpe: ColumnType {
                raw: "jsonb".to_string(),
                family: ColumnTypeFamily::Json,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "uuid_col".to_string(),
            tpe: ColumnType {
                raw: "uuid".to_string(),
                family: ColumnTypeFamily::Uuid,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
    ];
    expected_columns.sort_unstable_by_key(|c| c.name.to_owned());

    assert_eq!(
        table,
        Table {
            name: "User".to_string(),
            columns: expected_columns,
            indices: vec![],
            primary_key: Some(PrimaryKey {
                columns: vec!["primary_col".to_string()],
            }),
            foreign_keys: vec![],
        }
    );
}

#[test]
fn all_mysql_column_types_must_work() {
    setup();

    let mut migration = Migration::new().schema(SCHEMA);
    migration.create_table("User", move |t| {
        t.add_column("primary_col", types::primary());
        t.add_column("int_col", types::custom("int"));
        t.add_column("smallint_col", types::custom("smallint"));
        t.add_column("tinyint_col", types::custom("tinyint"));
        t.add_column("mediumint_col", types::custom("mediumint"));
        t.add_column("bigint_col", types::custom("bigint"));
        t.add_column("decimal_col", types::custom("decimal"));
        t.add_column("numeric_col", types::custom("numeric"));
        t.add_column("float_col", types::custom("float"));
        t.add_column("double_col", types::custom("double"));
        t.add_column("date_col", types::custom("date"));
        t.add_column("time_col", types::custom("time"));
        t.add_column("datetime_col", types::custom("datetime"));
        t.add_column("timestamp_col", types::custom("timestamp"));
        t.add_column("year_col", types::custom("year"));
        t.add_column("char_col", types::custom("char"));
        t.add_column("varchar_col", types::custom("varchar(255)"));
        t.add_column("text_col", types::custom("text"));
        t.add_column("tinytext_col", types::custom("tinytext"));
        t.add_column("mediumtext_col", types::custom("mediumtext"));
        t.add_column("longtext_col", types::custom("longtext"));
        t.add_column("enum_col", types::custom("enum('a', 'b')"));
        t.add_column("set_col", types::custom("set('a', 'b')"));
        t.add_column("binary_col", types::custom("binary"));
        t.add_column("varbinary_col", types::custom("varbinary(255)"));
        t.add_column("blob_col", types::custom("blob"));
        t.add_column("tinyblob_col", types::custom("tinyblob"));
        t.add_column("mediumblob_col", types::custom("mediumblob"));
        t.add_column("longblob_col", types::custom("longblob"));
        t.add_column("geometry_col", types::custom("geometry"));
        t.add_column("point_col", types::custom("point"));
        t.add_column("linestring_col", types::custom("linestring"));
        t.add_column("polygon_col", types::custom("polygon"));
        t.add_column("multipoint_col", types::custom("multipoint"));
        t.add_column("multilinestring_col", types::custom("multilinestring"));
        t.add_column("multipolygon_col", types::custom("multipolygon"));
        t.add_column("geometrycollection_col", types::custom("geometrycollection"));
        t.add_column("json_col", types::custom("json"));
    });

    let full_sql = migration.make::<barrel::backend::MySql>();
    let mut inspector = get_mysql_connector(&full_sql);
    let result = inspector.introspect(&SCHEMA.to_string()).expect("introspection");
    let mut table = result.get_table("User").expect("couldn't get User table").to_owned();
    // Ensure columns are sorted as expected when comparing
    table.columns.sort_unstable_by_key(|c| c.name.to_owned());
    let db_type = DbType::MySql;
    let mut expected_columns = vec![
        Column {
            name: "primary_col".to_string(),
            tpe: ColumnType {
                raw: "int".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: true,
        },
        Column {
            name: "int_col".to_string(),
            tpe: ColumnType {
                raw: "int".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "smallint_col".to_string(),
            tpe: ColumnType {
                raw: "smallint".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "tinyint_col".to_string(),
            tpe: ColumnType {
                raw: "tinyint".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "mediumint_col".to_string(),
            tpe: ColumnType {
                raw: "mediumint".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "bigint_col".to_string(),
            tpe: ColumnType {
                raw: "bigint".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "decimal_col".to_string(),
            tpe: ColumnType {
                raw: "decimal".to_string(),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "numeric_col".to_string(),
            tpe: ColumnType {
                raw: "decimal".to_string(),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "float_col".to_string(),
            tpe: ColumnType {
                raw: "float".to_string(),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "double_col".to_string(),
            tpe: ColumnType {
                raw: "double".to_string(),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "date_col".to_string(),
            tpe: ColumnType {
                raw: "date".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "time_col".to_string(),
            tpe: ColumnType {
                raw: "time".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "datetime_col".to_string(),
            tpe: ColumnType {
                raw: "datetime".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "timestamp_col".to_string(),
            tpe: ColumnType {
                raw: "timestamp".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: Some("CURRENT_TIMESTAMP".to_string()),
            auto_increment: false,
        },
        Column {
            name: "year_col".to_string(),
            tpe: ColumnType {
                raw: "year".to_string(),
                family: ColumnTypeFamily::DateTime,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "char_col".to_string(),
            tpe: ColumnType {
                raw: "char".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "varchar_col".to_string(),
            tpe: ColumnType {
                raw: "varchar".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "text_col".to_string(),
            tpe: ColumnType {
                raw: "text".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "tinytext_col".to_string(),
            tpe: ColumnType {
                raw: "tinytext".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "mediumtext_col".to_string(),
            tpe: ColumnType {
                raw: "mediumtext".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "longtext_col".to_string(),
            tpe: ColumnType {
                raw: "longtext".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "enum_col".to_string(),
            tpe: ColumnType {
                raw: "enum".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "set_col".to_string(),
            tpe: ColumnType {
                raw: "set".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "binary_col".to_string(),
            tpe: ColumnType {
                raw: "binary".to_string(),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "varbinary_col".to_string(),
            tpe: ColumnType {
                raw: "varbinary".to_string(),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "blob_col".to_string(),
            tpe: ColumnType {
                raw: "blob".to_string(),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "tinyblob_col".to_string(),
            tpe: ColumnType {
                raw: "tinyblob".to_string(),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "mediumblob_col".to_string(),
            tpe: ColumnType {
                raw: "mediumblob".to_string(),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "longblob_col".to_string(),
            tpe: ColumnType {
                raw: "longblob".to_string(),
                family: ColumnTypeFamily::Binary,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "geometry_col".to_string(),
            tpe: ColumnType {
                raw: "geometry".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "point_col".to_string(),
            tpe: ColumnType {
                raw: "point".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "linestring_col".to_string(),
            tpe: ColumnType {
                raw: "linestring".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "polygon_col".to_string(),
            tpe: ColumnType {
                raw: "polygon".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "multipoint_col".to_string(),
            tpe: ColumnType {
                raw: "multipoint".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "multilinestring_col".to_string(),
            tpe: ColumnType {
                raw: "multilinestring".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "multipolygon_col".to_string(),
            tpe: ColumnType {
                raw: "multipolygon".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "geometrycollection_col".to_string(),
            tpe: ColumnType {
                raw: "geometrycollection".to_string(),
                family: ColumnTypeFamily::Geometric,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "json_col".to_string(),
            tpe: ColumnType {
                raw: "json".to_string(),
                family: ColumnTypeFamily::Json,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
    ];
    expected_columns.sort_unstable_by_key(|c| c.name.to_owned());

    assert_eq!(
        table,
        Table {
            name: "User".to_string(),
            columns: expected_columns,
            indices: vec![],
            primary_key: Some(PrimaryKey {
                columns: vec!["primary_col".to_string()],
            }),
            foreign_keys: vec![],
        }
    );
}

#[test]
fn sqlite_column_types_must_work() {
    setup();

    let mut migration = Migration::new().schema(SCHEMA);
    migration.create_table("User", move |t| {
        t.add_column("int4_col", types::integer());
        t.add_column("text_col", types::text());
        t.add_column("real_col", types::float());
        t.add_column("primary_col", types::primary());
    });

    let full_sql = migration.make::<barrel::backend::Pg>();
    let mut inspector = get_sqlite_connector(&full_sql);
    let result = inspector.introspect(SCHEMA).expect("introspection");
    let table = result.get_table("User").expect("couldn't get User table");
    let db_type = DbType::Sqlite;
    let mut expected_columns = vec![
        Column {
            name: "int4_col".to_string(),
            tpe: ColumnType {
                raw: "INTEGER".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "text_col".to_string(),
            tpe: ColumnType {
                raw: "TEXT".to_string(),
                family: ColumnTypeFamily::String,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "real_col".to_string(),
            tpe: ColumnType {
                raw: "FLOAT".to_string(),
                family: ColumnTypeFamily::Float,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: false,
        },
        Column {
            name: "primary_col".to_string(),
            tpe: ColumnType {
                raw: "SERIAL".to_string(),
                family: ColumnTypeFamily::Int,
            },
            arity: ColumnArity::Required,
            default: None,
            auto_increment: true,
        },
    ];
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
            let result = inspector.introspect(SCHEMA).expect("introspecting");
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
                    auto_increment: false,
                },
                Column {
                    name: "column2".to_string(),
                    tpe: ColumnType {
                        raw: int_type(db_type),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Nullable,
                    default: None,
                    auto_increment: false,
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
            migration.create_table("City", |t| {
                t.add_column("id", types::primary());
            });
            migration.create_table("User", move |t| {
                // barrel does not render foreign keys correctly for mysql
                // TODO: Investigate
                if db_type == DbType::MySql {
                    t.add_column("city", types::integer());
                    t.inject_custom("FOREIGN KEY(city) REFERENCES City(id)");
                } else {
                    t.add_column("city", types::foreign("City", "id"));
                }
            });
        },
        |db_type, inspector| {
            let schema = inspector.introspect(SCHEMA).expect("introspection");
            let user_table = schema.get_table("User").expect("couldn't get User table");
            let expected_columns = vec![Column {
                name: "city".to_string(),
                tpe: ColumnType {
                    raw: int_type(db_type),
                    family: ColumnTypeFamily::Int,
                },
                arity: ColumnArity::Required,
                default: None,
                auto_increment: false,
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
                        on_delete_action: ForeignKeyAction::NoAction,
                    }],
                }
            );
        },
    );
}

#[test]
fn multi_column_foreign_keys_must_work() {
    setup();

    test_each_backend(
        |db_type, mut migration| {
            migration.create_table("City", move |t| {
                t.add_column("id", types::primary());
                t.add_column("name", types::varchar(255));
                if db_type != DbType::Sqlite {
                    t.inject_custom("constraint uniq unique (id, name)");
                }
            });
            migration.create_table("User", move |t| {
                t.add_column("city", types::integer());
                t.add_column("city_name", types::varchar(255));
                if db_type == DbType::MySql {
                    t.inject_custom("FOREIGN KEY(city, city_name) REFERENCES City(id, name)");
                } else {
                    let relation_prefix = match db_type {
                        DbType::Postgres => format!("\"{}\".", SCHEMA),
                        _ => "".to_string(),
                    };
                    t.inject_custom(format!(
                        "FOREIGN KEY(city, city_name) REFERENCES {}\"City\"(id, name)",
                        relation_prefix
                    ));
                }
            });
        },
        |db_type, inspector| {
            let schema = inspector.introspect(SCHEMA).expect("introspection");
            let user_table = schema.get_table("User").expect("couldn't get User table");
            let expected_columns = vec![
                Column {
                    name: "city".to_string(),
                    tpe: ColumnType {
                        raw: int_type(db_type),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: false,
                },
                Column {
                    name: "city_name".to_string(),
                    tpe: ColumnType {
                        raw: varchar_type(db_type, 255),
                        family: ColumnTypeFamily::String,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: false,
                },
            ];

            assert_eq!(
                user_table,
                &Table {
                    name: "User".to_string(),
                    columns: expected_columns,
                    indices: vec![],
                    primary_key: None,
                    foreign_keys: vec![ForeignKey {
                        columns: vec!["city".to_string(), "city_name".to_string()],
                        referenced_columns: vec!["id".to_string(), "name".to_string()],
                        referenced_table: "City".to_string(),
                        on_delete_action: ForeignKeyAction::NoAction,
                    },],
                }
            );
        },
    );
}

#[test]
fn postgres_foreign_key_on_delete_must_be_handled() {
    setup();

    let sql = format!(
        "CREATE TABLE \"{0}\".\"City\" (id INT PRIMARY KEY);
         CREATE TABLE \"{0}\".\"User\" (
            id INT PRIMARY KEY, 
            city INT REFERENCES \"{0}\".\"City\" (id) ON DELETE NO ACTION,
            city_cascade INT REFERENCES \"{0}\".\"City\" (id) ON DELETE CASCADE,
            city_restrict INT REFERENCES \"{0}\".\"City\" (id) ON DELETE RESTRICT,
            city_set_null INT REFERENCES \"{0}\".\"City\" (id) ON DELETE SET NULL,
            city_set_default INT REFERENCES \"{0}\".\"City\" (id) ON DELETE SET DEFAULT
        );
        ",
        SCHEMA
    );
    let mut inspector = get_postgres_connector(&sql);

    let schema = inspector.introspect(SCHEMA).expect("introspection");
    let mut table = schema.get_table("User").expect("get User table").to_owned();
    table.foreign_keys.sort_unstable_by_key(|fk| fk.columns.clone());

    assert_eq!(
        table,
        Table {
            name: "User".to_string(),
            columns: vec![
                Column {
                    name: "city".to_string(),
                    tpe: ColumnType {
                        raw: "int4".to_string(),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Nullable,
                    default: None,
                    auto_increment: false,
                },
                Column {
                    name: "city_cascade".to_string(),
                    tpe: ColumnType {
                        raw: "int4".to_string(),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Nullable,
                    default: None,
                    auto_increment: false,
                },
                Column {
                    name: "city_restrict".to_string(),
                    tpe: ColumnType {
                        raw: "int4".to_string(),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Nullable,
                    default: None,
                    auto_increment: false,
                },
                Column {
                    name: "city_set_default".to_string(),
                    tpe: ColumnType {
                        raw: "int4".to_string(),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Nullable,
                    default: None,
                    auto_increment: false,
                },
                Column {
                    name: "city_set_null".to_string(),
                    tpe: ColumnType {
                        raw: "int4".to_string(),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Nullable,
                    default: None,
                    auto_increment: false,
                },
                Column {
                    name: "id".to_string(),
                    tpe: ColumnType {
                        raw: "int4".to_string(),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: false,
                },
            ],
            indices: vec![],
            primary_key: Some(PrimaryKey {
                columns: vec!["id".to_string()],
            }),
            foreign_keys: vec![
                ForeignKey {
                    columns: vec!["city".to_string()],
                    referenced_columns: vec!["id".to_string()],
                    referenced_table: "City".to_string(),
                    on_delete_action: ForeignKeyAction::NoAction,
                },
                ForeignKey {
                    columns: vec!["city_cascade".to_string()],
                    referenced_columns: vec!["id".to_string()],
                    referenced_table: "City".to_string(),
                    on_delete_action: ForeignKeyAction::Cascade,
                },
                ForeignKey {
                    columns: vec!["city_restrict".to_string()],
                    referenced_columns: vec!["id".to_string()],
                    referenced_table: "City".to_string(),
                    on_delete_action: ForeignKeyAction::Restrict,
                },
                ForeignKey {
                    columns: vec!["city_set_default".to_string()],
                    referenced_columns: vec!["id".to_string()],
                    referenced_table: "City".to_string(),
                    on_delete_action: ForeignKeyAction::SetDefault,
                },
                ForeignKey {
                    columns: vec!["city_set_null".to_string()],
                    referenced_columns: vec!["id".to_string()],
                    referenced_table: "City".to_string(),
                    on_delete_action: ForeignKeyAction::SetNull,
                },
            ],
        }
    );
}

#[test]
fn postgres_enums_must_work() {
    setup();

    let mut inspector = get_postgres_connector(&format!(
        "CREATE TYPE \"{}\".\"mood\" AS ENUM ('sad', 'ok', 'happy')",
        SCHEMA
    ));

    let schema = inspector.introspect(SCHEMA).expect("introspection");
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

    let schema = inspector.introspect(SCHEMA).expect("introspection");
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

#[test]
fn indices_must_work() {
    setup();

    test_each_backend(
        |db_type, mut migration| {
            migration.create_table("User", move |t| {
                t.add_column("id", types::primary());
                t.add_column("count", types::integer());
                t.add_index("count", types::index(vec!["count"]));
            });
        },
        |db_type, inspector| {
            let result = inspector.introspect(&SCHEMA.to_string()).expect("introspecting");
            let user_table = result.get_table("User").expect("getting User table");
            let default = match db_type {
                DbType::Postgres => Some(format!("nextval('\"{}\".\"User_id_seq\"'::regclass)", SCHEMA)),
                _ => None,
            };
            let expected_columns = vec![
                Column {
                    name: "count".to_string(),
                    tpe: ColumnType {
                        raw: int_type(db_type),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default: None,
                    auto_increment: false,
                },
                Column {
                    name: "id".to_string(),
                    tpe: ColumnType {
                        raw: int_type(db_type),
                        family: ColumnTypeFamily::Int,
                    },
                    arity: ColumnArity::Required,
                    default,
                    auto_increment: true,
                },
            ];
            assert_eq!(
                user_table,
                &Table {
                    name: "User".to_string(),
                    columns: expected_columns,
                    indices: vec![Index {
                        name: "count".to_string(),
                        columns: vec!["count".to_string()],
                        unique: false,
                    },],
                    primary_key: Some(PrimaryKey {
                        columns: vec!["id".to_string()],
                    }),
                    foreign_keys: vec![],
                }
            );
        },
    );
}

fn test_each_backend<MigrationFn, TestFn>(mut migrationFn: MigrationFn, testFn: TestFn)
where
    MigrationFn: FnMut(DbType, &mut Migration) -> (),
    TestFn: Fn(DbType, &mut dyn IntrospectionConnector) -> (),
{
    // SQLite
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn(DbType::Sqlite, &mut migration);
        let full_sql = migration.make::<barrel::backend::Sqlite>();
        let mut inspector = get_sqlite_connector(&full_sql);

        testFn(DbType::Sqlite, &mut inspector);
    }
    // Postgres
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn(DbType::Postgres, &mut migration);
        let full_sql = migration.make::<barrel::backend::Pg>();
        let mut inspector = get_postgres_connector(&full_sql);

        testFn(DbType::Postgres, &mut inspector);
    }
    // MySQL
    {
        let mut migration = Migration::new().schema(SCHEMA);
        migrationFn(DbType::MySql, &mut migration);
        let full_sql = migration.make::<barrel::backend::MySql>();
        let mut inspector = get_mysql_connector(&full_sql);

        testFn(DbType::MySql, &mut inspector);
    }
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
    let database_file_path = format!("{}/{}.db", database_folder_path, SCHEMA);
    debug!("Database file path: '{}'", database_file_path);
    if Path::new(&database_file_path).exists() {
        std::fs::remove_file(database_file_path.clone()).expect("remove database file");
    }

    let conn = rusqlite::Connection::open_in_memory().expect("opening SQLite connection should work");
    conn.execute(
        "ATTACH DATABASE ? as ?",
        &vec![database_file_path.clone(), String::from(SCHEMA)],
    )
    .expect("attach SQLite database");
    debug!("Executing migration: {}", sql);
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
    let host = match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db-postgres",
        Err(_) => "127.0.0.1",
    };
    let mut client = ::postgres::Config::new()
        .user("postgres")
        .password("prisma")
        .host(host)
        .port(5432)
        .dbname("postgres")
        .connect(::postgres::NoTls)
        .expect("connecting to Postgres");

    let drop_schema = format!("DROP SCHEMA IF EXISTS \"{}\" CASCADE;", SCHEMA);
    client.execute(drop_schema.as_str(), &[]).expect("dropping schema");

    debug!("Creating Postgres schema '{}'", SCHEMA);
    client
        .execute(format!("CREATE SCHEMA \"{}\";", SCHEMA).as_str(), &[])
        .expect("creating schema");

    let sql_string = sql.to_string();
    let statements: Vec<&str> = sql_string.split(";").filter(|s| !s.is_empty()).collect();
    for statement in statements {
        debug!("Executing migration statement: '{}'", statement);
        client.execute(statement, &[]).expect("executing migration statement");
    }

    let conn = Arc::new(PostgresConnection {
        client: Mutex::new(prisma_query::connector::PostgreSql::from(client)),
    });
    postgres::IntrospectionConnector::new(conn)
}

struct MySqlConnection {
    client: Mutex<prisma_query::connector::Mysql>,
}

impl crate::IntrospectionConnection for MySqlConnection {
    fn query_raw(&self, sql: &str, schema: &str) -> prisma_query::Result<prisma_query::connector::ResultSet> {
        self.client.lock().expect("self.client.lock").query_raw(sql, &[])
    }
}

fn get_mysql_connector(sql: &str) -> mysql::IntrospectionConnector {
    let host = match std::env::var("IS_BUILDKITE") {
        Ok(_) => "test-db-mysql",
        Err(_) => "127.0.0.1",
    };
    let port = 3306;
    let user = "root";
    let password = "prisma";

    debug!("Connecting to MySQL server at {}, port {}, user '{}'", host, port, user);

    let mut opts_builder = prisma_query::pool::mysql::OptsBuilder::new();
    opts_builder
        .ip_or_hostname(Some(host))
        .tcp_port(port)
        .user(Some(user))
        .pass(Some(password));
    let mut conn = prisma_query::connector::Mysql::new(opts_builder).expect("connect to MySQL");

    conn.execute_raw(&format!("DROP SCHEMA IF EXISTS {}", SCHEMA), &[])
        .expect("dropping schema");
    conn.execute_raw(&format!("CREATE SCHEMA {}", SCHEMA), &[])
        .expect("creating schema");

    debug!("Executing MySQL migrations: {}", sql);
    let sql_string = sql.to_string();
    let statements: Vec<&str> = sql_string.split(";").filter(|s| !s.is_empty()).collect();
    for statement in statements {
        debug!("Executing migration statement: '{}'", statement);
        conn.execute_raw(&statement, &[])
            .expect("executing migration statement");
    }

    let mut opts_builder = prisma_query::pool::mysql::OptsBuilder::new();
    opts_builder
        .ip_or_hostname(Some(host))
        .tcp_port(port)
        .user(Some(user))
        .pass(Some(password))
        .db_name(Some(SCHEMA));
    let mut conn = prisma_query::connector::Mysql::new(opts_builder).expect("connect to MySQL");

    mysql::IntrospectionConnector::new(Arc::new(MySqlConnection {
        client: Mutex::new(conn),
    }))
}
