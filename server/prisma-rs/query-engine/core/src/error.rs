use connector::ConnectorError;
use failure::Fail;
use prisma_models::DomainError;

#[derive(Debug, Fail)]
pub enum CoreError {
    #[fail(display = "Error in connector: {}", _0)]
    ConnectorError(ConnectorError),

    #[fail(display = "Error in domain logic: {}", _0)]
    DomainError(DomainError),

    #[fail(display = "Query validation error: {}", _0)]
    QueryValidationError(String),
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
