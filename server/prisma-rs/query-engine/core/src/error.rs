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
    AssertionError(String), // Naming is WIP. Denotes a generic validation error.
    RequiredValueNotSetError,
    FieldNotFoundError,
    ArgumentNotFoundError,
    AtLeastOneSelectionError,
    ValueParseError(String),
    InputFieldValidationError,
    ValueTypeMismatchError {
        have: QueryValue,
        want: InputType,
    },
    ArgumentValidationError {
        argument: String,
        inner: Box<QueryValidationError>,
    },
    FieldValidationError {
        field_name: String,
        inner: Box<QueryValidationError>,
    },
    ObjectValidationError {
        object_name: String,
        inner: Box<QueryValidationError>,
    },
}

impl fmt::Display for QueryValidationError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "Query validation error: {:?}", self)
    }
}

impl From<QueryValidationError> for CoreError {
    fn from(e: QueryValidationError) -> CoreError {
        CoreError::QueryValidationError(e)
    }
}
