use failure::{Error, Fail};
use libsqlite3_sys as ffi;
use prisma_models::prelude::{DomainError, PrismaValue};
use rusqlite;

#[derive(Debug, Fail)]
pub enum ConnectorError {
    #[fail(display = "Unique constraint failed: {}", field_name)]
    UniqueConstraintViolation { field_name: String },
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
    #[fail(display = "Node not found, where field {} is {}", field, value)]
    NodeNotFoundForWhere { field: String, value: PrismaValue },
    #[fail(display = "Field cannot be null: {}", field)]
    FieldCannotBeNull { field: String },
    #[fail(display = "{}", _0)]
    DomainError(DomainError),
}

impl From<DomainError> for ConnectorError {
    fn from(e: DomainError) -> ConnectorError {
        ConnectorError::DomainError(e)
    }
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
                Some(description),
            ) => {
                let splitted: Vec<&str> = description.split(": ").collect();

                ConnectorError::UniqueConstraintViolation {
                    field_name: splitted[1].into(),
                }
            }

            rusqlite::Error::SqliteFailure(
                ffi::Error {
                    code: ffi::ErrorCode::ConstraintViolation,
                    extended_code: 1555,
                },
                Some(description),
            ) => {
                let splitted: Vec<&str> = description.split(": ").collect();

                ConnectorError::UniqueConstraintViolation {
                    field_name: splitted[1].into(),
                }
            }

            e => ConnectorError::QueryError(e.into()),
        }
    }
}

impl From<uuid::parser::ParseError> for ConnectorError {
    fn from(e: uuid::parser::ParseError) -> ConnectorError {
        ConnectorError::ColumnReadFailure(e.into())
    }
}
