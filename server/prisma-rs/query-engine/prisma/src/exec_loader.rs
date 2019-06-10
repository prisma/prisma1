use crate::{PrismaError, PrismaResult};
use core::{Executor, ReadQueryExecutor, WriteQueryExecutor};
use datamodel::{
    source::{MySqlSource, PostgresSource, SqliteSource, MYSQL_SOURCE_NAME, POSTGRES_SOURCE_NAME, SQLITE_SOURCE_NAME},
    Source,
};
use std::sync::Arc;
use std::{convert::TryFrom, path::PathBuf};

#[cfg(feature = "sql")]
use sql_connector::{Mysql, PostgreSql, SqlDatabase, Sqlite, Transactional};

pub fn load(source: &Box<dyn Source>) -> PrismaResult<Executor> {
    match source.connector_type() {
        #[cfg(feature = "sql")]
        SQLITE_SOURCE_NAME => sqlite(source),

        #[cfg(feature = "sql")]
        MYSQL_SOURCE_NAME => unimplemented!(),

        #[cfg(feature = "sql")]
        POSTGRES_SOURCE_NAME => unimplemented!(),

        x => Err(PrismaError::ConfigurationError(format!(
            "Unsupported connector type: {}",
            x
        ))),
    }

    // match config.databases.get("default") {
    //     #[cfg(feature = "sql")]
    //     Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite-native" => sqlite(config),

    //     #[cfg(feature = "sql")]
    //     Some(config) if config.connector() == "postgres-native" => postgres(config),

    //     #[cfg(feature = "sql")]
    //     Some(config) if config.connector() == "mysql-native" => mysql(config),

    //     Some(config) => panic!("Database connector for {} is not supported.", config.connector()),

    //     None => panic!("Default database not set."),
    // }
}

#[cfg(feature = "sql")]
fn sqlite(source: &Box<dyn Source>) -> PrismaResult<Executor> {
    let sqlite = Sqlite::try_from(source)?;
    let db = SqlDatabase::new(sqlite);
    let path = PathBuf::from(source.url());
    let db_name = path.file_stem().unwrap(); // Safe due to previous validations.

    Ok(sql_executor(db_name.to_os_string().into_string().unwrap(), db))
}

// #[cfg(feature = "sql")]
// fn postgres(config: &PrismaDatabase) -> Executor {
//     let postgres = PostgreSql::try_from(config).unwrap();
//     let connector = SqlDatabase::new(postgres);

//     sql_executor("".into(), connector)
// }

// #[cfg(feature = "sql")]
// fn mysql(config: &PrismaDatabase) -> Executor {
//     let postgres = Mysql::try_from(config).unwrap();
//     let connector = SqlDatabase::new(postgres);

//     sql_executor("".into(), connector)
// }

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
