use core::{Executor, ReadQueryExecutor, WriteQueryExecutor};
use prisma_common::config::{ConnectionLimit, FileConfig, PrismaConfig, PrismaDatabase};
use std::convert::TryFrom;
use std::sync::Arc;

#[cfg(feature = "sql")]
use sql_connector::{Mysql, PostgreSql, SqlDatabase, Sqlite, Transactional};

pub fn load(config: &PrismaConfig) -> Executor {
    match config.databases.get("default") {
        #[cfg(feature = "sql")]
        Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite-native" => sqlite(config),

        #[cfg(feature = "sql")]
        Some(config) if config.connector() == "postgres-native" => postgres(config),

        #[cfg(feature = "sql")]
        Some(config) if config.connector() == "mysql-native" => mysql(config),

        Some(config) => panic!("Database connector for {} is not supported.", config.connector()),

        None => panic!("Default database not set."),
    }
}

#[cfg(feature = "sql")]
fn sqlite(config: &FileConfig) -> Executor {
    let db_name = config.db_name();
    let db_folder = config
        .database_file
        .trim_end_matches(&format!("{}.db", db_name))
        .trim_end_matches("/");

    let sqlite = Sqlite::new(db_folder.to_owned(), config.limit(), false).unwrap();
    // let arc = Arc::new(SqlDatabase::new(sqlite));
    let wat = SqlDatabase::new(sqlite);

    // sql_executor(db_name.clone(), Arc::clone(&arc), arc)
    sql_executor(db_name.clone(), wat)
}

#[cfg(feature = "sql")]
fn postgres(config: &PrismaDatabase) -> Executor {
    let postgres = PostgreSql::try_from(config).unwrap();
    let connector = SqlDatabase::new(postgres);

    sql_executor("".into(), connector)
}

#[cfg(feature = "sql")]
fn mysql(config: &PrismaDatabase) -> Executor {
    let postgres = Mysql::try_from(config).unwrap();
    let connector = SqlDatabase::new(postgres);

    sql_executor("".into(), connector)
}

#[cfg(feature = "sql")]
fn sql_executor<T>(db_name: String, connector: SqlDatabase<T>) -> Executor
where
    T: Transactional + Send + Sync + 'static,
{
    let arc = Arc::new(connector);
    let read_exec: ReadQueryExecutor = ReadQueryExecutor {
        data_resolver: arc.clone(),
    };
    let write_exec: WriteQueryExecutor = WriteQueryExecutor {
        db_name: db_name,
        write_executor: arc,
    };

    Executor { read_exec, write_exec }
}
