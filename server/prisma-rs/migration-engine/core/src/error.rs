use failure::{Error as Schwerror, Fail};
use sql_migration_connector::SqlError;
use migration_connector::ConnectorError;
use crate::commands::CommandError;
use datamodel::errors::ErrorCollection;
use tokio_threadpool::BlockingError;

#[derive(Debug, Fail)]
pub enum Error {
    #[fail(display = "Error in connector: {}", _0)]
    ConnectorError(Schwerror),

    #[fail(display = "Failure during a migration command: {}", _0)]
    CommandError(CommandError),

    #[fail(display = "Error in datamodel: {:?}", _0)]
    DatamodelError(ErrorCollection),

    #[fail(display = "Error performing IO: {:?}", _0)]
    IOError(Schwerror),

    #[fail(display = "Threadpool error: {:?}", _0)]
    BlockingError(BlockingError),
}

impl From<BlockingError> for Error {
    fn from(e: BlockingError) -> Self {
        Error::BlockingError(e)
    }
}

impl From<SqlError> for Error {
    fn from(e: SqlError) -> Self {
        Error::ConnectorError(e.into())
    }
}

impl From<ConnectorError> for Error {
    fn from(e: ConnectorError) -> Self {
        Error::ConnectorError(e.into())
    }
}

impl From<CommandError> for Error {
    fn from(e: CommandError) -> Self {
        Error::CommandError(e)
    }
}

impl From<ErrorCollection> for Error {
    fn from(e: ErrorCollection) -> Self {
        Error::DatamodelError(e)
    }
}

impl From<std::io::Error> for Error {
    fn from(e: std::io::Error) -> Self {
        Error::IOError(e.into())
    }
}
