use failure::{Error, Fail};
use libsqlite3_sys as ffi;
use rusqlite;

#[derive(Debug, Fail)]
pub enum ConnectorError {
    #[fail(display = "Unique constraint violation in model.")]
    UniqueConstraintViolation,
    #[fail(display = "Node does not exist.")]
    NodeDoesNotExist,
    #[fail(display = "Error creating a database connection.")]
    ConnectionError(Error),
    #[fail(display = "Error querying the database.")]
    QueryError(Error),
    #[fail(display = "The provided arguments are not supported.")]
    InvalidConnectionArguments,
    #[fail(display = "The column value was different from the model")]
    ColumnReadFailure(Error),
}

#[cfg(feature = "sql")]
impl From<r2d2::Error> for ConnectorError {
    fn from(e: r2d2::Error) -> ConnectorError {
        ConnectorError::ConnectionError(e.into())
    }
}

#[cfg(feature = "sqlite")]
impl From<rusqlite::Error> for ConnectorError {
    fn from(e: rusqlite::Error) -> ConnectorError {
        match e {
            rusqlite::Error::QueryReturnedNoRows => ConnectorError::NodeDoesNotExist,

            rusqlite::Error::SqliteFailure(
                ffi::Error {
                    code: ffi::ErrorCode::ConstraintViolation,
                    extended_code: 2067,
                },
                _,
            ) => ConnectorError::UniqueConstraintViolation,

            e => ConnectorError::QueryError(e.into()),
        }
    }
}

impl From<uuid::parser::ParseError> for ConnectorError {
    fn from(e: uuid::parser::ParseError) -> ConnectorError {
        ConnectorError::ColumnReadFailure(e.into())
    }
}
