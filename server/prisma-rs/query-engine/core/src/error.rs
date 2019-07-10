use crate::{query_builders::QueryValidationError};
use connector::error::ConnectorError;
use failure::Fail;
use prisma_models::DomainError;

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

impl From<QueryValidationError> for CoreError {
    fn from(e: QueryValidationError) -> CoreError {
        CoreError::QueryValidationError(e)
    }
}