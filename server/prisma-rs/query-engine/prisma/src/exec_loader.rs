use crate::{PrismaError, PrismaResult};
use core::{Executor, ReadQueryExecutor, WriteQueryExecutor};
use datamodel::{
    configuration::{MYSQL_SOURCE_NAME, POSTGRES_SOURCE_NAME, SQLITE_SOURCE_NAME},
    Source,
};
use std::{convert::TryFrom, path::PathBuf, sync::Arc};
use url::Url;

#[cfg(feature = "sql")]
use sql_connector::{Mysql, PostgreSql, SqlDatabase, Sqlite, Transactional};

pub fn load(source: &Box<dyn Source>) -> PrismaResult<Executor> {
    match source.connector_type() {
        #[cfg(feature = "sql")]
        SQLITE_SOURCE_NAME => sqlite(source),

        #[cfg(feature = "sql")]
        MYSQL_SOURCE_NAME => mysql(source),

        #[cfg(feature = "sql")]
        POSTGRES_SOURCE_NAME => postgres(source),

        x => Err(PrismaError::ConfigurationError(format!(
            "Unsupported connector type: {}",
            x
        ))),
    }
}

#[cfg(feature = "sql")]
fn sqlite(source: &Box<dyn Source>) -> PrismaResult<Executor> {
    let sqlite = Sqlite::try_from(source)?;
    let db = SqlDatabase::new(sqlite);
    let path = PathBuf::from(source.url());
    let db_name = path.file_stem().unwrap(); // Safe due to previous validations.

    Ok(sql_executor(db_name.to_os_string().into_string().unwrap(), db))
}

#[cfg(feature = "sql")]
fn postgres(source: &Box<dyn Source>) -> PrismaResult<Executor> {
    let psql = PostgreSql::try_from(source)?;
    let db = SqlDatabase::new(psql);
    let url = Url::parse(source.url())?;
    let err_str = "No database found in connection string";
    let mut db_name = url
        .path_segments()
        .ok_or(PrismaError::ConfigurationError(err_str.into()))?;
    let db_name = db_name.next().expect(err_str);

    Ok(sql_executor(db_name.into(), db))
}

#[cfg(feature = "sql")]
fn mysql(source: &Box<dyn Source>) -> PrismaResult<Executor> {
    let psql = Mysql::try_from(source)?;
    let db = SqlDatabase::new(psql);
    let url = Url::parse(source.url())?;
    let err_str = "No database found in connection string";
    let mut db_name = url
        .path_segments()
        .ok_or(PrismaError::ConfigurationError(err_str.into()))?;
    let db_name = db_name.next().expect(err_str);

    Ok(sql_executor(db_name.into(), db))
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
