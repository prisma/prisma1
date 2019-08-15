use crate::{PrismaError, PrismaResult};
use core::executor::{QueryExecutor, ReadQueryExecutor, WriteQueryExecutor};
use datamodel::{
    configuration::{MYSQL_SOURCE_NAME, POSTGRES_SOURCE_NAME, SQLITE_SOURCE_NAME},
    Source,
};
use std::{collections::HashMap, path::PathBuf, sync::Arc};
use url::Url;

#[cfg(feature = "sql")]
use sql_connector::*;

pub fn load(source: &dyn Source) -> PrismaResult<QueryExecutor> {
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
fn sqlite(source: &dyn Source) -> PrismaResult<QueryExecutor> {
    trace!("Loading SQLite connector...");

    let sqlite = Sqlite::from_source(source)?;
    let path = PathBuf::from(sqlite.file_path());
    let db = SqlDatabase::new(sqlite);
    let db_name = path.file_stem().unwrap(); // Safe due to previous validations.

    trace!("Loaded SQLite connector.");
    Ok(sql_executor(db_name.to_os_string().into_string().unwrap(), db))
}

#[cfg(feature = "sql")]
fn postgres(source: &dyn Source) -> PrismaResult<QueryExecutor> {
    trace!("Loading Postgres connector...");

    let url = Url::parse(source.url())?;
    let params: HashMap<String, String> = url.query_pairs().into_owned().collect();

    let db_name = params
        .get("schema")
        .map(ToString::to_string)
        .unwrap_or_else(|| String::from("public"));

    let psql = PostgreSql::from_source(source)?;
    let db = SqlDatabase::new(psql);

    trace!("Loaded Postgres connector.");
    Ok(sql_executor(db_name, db))
}

#[cfg(feature = "sql")]
fn mysql(source: &dyn Source) -> PrismaResult<QueryExecutor> {
    trace!("Loading MySQL connector...");

    let psql = Mysql::from_source(source)?;
    let db = SqlDatabase::new(psql);
    let url = Url::parse(source.url())?;
    let err_str = "No database found in connection string";

    let mut db_name = url
        .path_segments()
        .ok_or_else(|| PrismaError::ConfigurationError(err_str.into()))?;

    let db_name = db_name.next().expect(err_str);

    trace!("Loaded MySQL connector.");
    Ok(sql_executor(db_name.into(), db))
}

#[cfg(feature = "sql")]
fn sql_executor<T>(db_name: String, connector: SqlDatabase<T>) -> QueryExecutor
where
    T: Transactional + SqlCapabilities + Send + Sync + 'static,
{
    let arc = Arc::new(connector);
    let read_exec: ReadQueryExecutor = ReadQueryExecutor {
        data_resolver: arc.clone(),
    };

    let write_exec: WriteQueryExecutor = WriteQueryExecutor {
        db_name,
        write_executor: arc,
    };

    QueryExecutor::new(read_exec, write_exec)
}
