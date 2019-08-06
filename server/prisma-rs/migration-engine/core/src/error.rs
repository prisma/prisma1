use failure::{Error as Schwerror, Fail};
use sql_migration_connector::SqlError;
use crate::commands::CommandError;
use datamodel::errors::ErrorCollection;

#[derive(Debug, Fail)]
pub enum Error {
    #[fail(display = "Error in connector: {}", _0)]
    ConnectorError(Schwerror),

    #[fail(display = "Failure during a migration command: {}", _0)]
    CommandError(CommandError),

    #[fail(display = "Error in datamodel: {:?}", _0)]
    DatamodelError(ErrorCollection),
}

impl From<SqlError> for Error {
    fn from(e: SqlError) -> Self {
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
