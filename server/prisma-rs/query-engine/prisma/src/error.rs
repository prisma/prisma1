use core::CoreError;
use failure::{Error, Fail};
use serde_json;

#[derive(Debug, Fail)]
pub enum PrismaError {
    #[fail(display = "{}", _0)]
    QueryParsingError(String),
    #[fail(display = "{}", _0)]
    QueryValidationError(String),
    #[fail(display = "{}", _0)]
    SerializationError(String),
    #[fail(display = "{}", _0)]
    CoreError(CoreError),
    #[fail(display = "{}", _0)]
    JsonDecodeError(Error),
}

impl From<CoreError> for PrismaError {
    fn from(e: CoreError) -> PrismaError {
        PrismaError::CoreError(e)
    }
}

impl From<serde_json::error::Error> for PrismaError {
    fn from(e: serde_json::error::Error) -> PrismaError {
        PrismaError::JsonDecodeError(e.into())
    }
}
