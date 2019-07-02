use crate::{query_document::QueryValue, InputType};
use connector::error::ConnectorError;
use failure::Fail;
use prisma_models::DomainError;
use std::fmt;

#[derive(Debug, Fail)]
pub enum CoreError {
    #[fail(display = "Error in connector: {}", _0)]
    ConnectorError(ConnectorError),

    #[fail(display = "Error in domain logic: {}", _0)]
    DomainError(DomainError),

    #[fail(display = "{}", _0)]
    QueryValidationError(QueryValidationError),

    #[fail(display = "{}", _0)]
    LegacyQueryValidationError(String),

    #[fail(display = "Unsupported feature: {}", _0)]
    UnsupportedFeatureError(String),
}

impl From<ConnectorError> for CoreError {
    fn from(e: ConnectorError) -> CoreError {
        CoreError::ConnectorError(e)
    }
}

impl From<DomainError> for CoreError {
    fn from(e: DomainError) -> CoreError {
        CoreError::DomainError(e)
    }
}

#[derive(Debug)]
pub enum QueryValidationError {
    RequiredValueNotSetError,
    ValueTypeMismatchError {
        have: QueryValue,
        want: InputType,
    },
    ValueParseError(String),
    FieldNotFoundError,
    ArgumentValidationError {
        argument: String,
        inner: Box<QueryValidationError>,
    },
    FieldValidationError {
        field_name: String,
        reason: Box<QueryValidationError>,
        on_object: String,
    },
    InputFieldValidationError,
}

impl fmt::Display for QueryValidationError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "Query validation error")
    }
}

impl From<QueryValidationError> for CoreError {
    fn from(e: QueryValidationError) -> CoreError {
        CoreError::QueryValidationError(e)
    }
}
