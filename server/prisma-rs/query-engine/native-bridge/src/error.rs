use connector::ConnectorError;
use failure::{Error, Fail};
use prisma_models::DomainError;
use prost::DecodeError;
use serde_json;

#[derive(Debug, Fail)]
pub enum BridgeError {
    #[fail(display = "Error in connector.")]
    ConnectorError(ConnectorError),
    #[fail(display = "Error in domain logic.")]
    DomainError(DomainError),
    #[fail(display = "Error decoding Protobuf input.")]
    ProtobufDecodeError(Error),
    #[fail(display = "Error decoding JSON input.")]
    JsonDecodeError(Error),
    #[fail(display = "Error decoding JSON input.")]
    InvalidConnectionArguments(&'static str),
}

impl From<ConnectorError> for BridgeError {
    fn from(e: ConnectorError) -> BridgeError {
        BridgeError::ConnectorError(e)
    }
}

impl From<DomainError> for BridgeError {
    fn from(e: DomainError) -> BridgeError {
        BridgeError::DomainError(e)
    }
}

impl From<DecodeError> for BridgeError {
    fn from(e: DecodeError) -> BridgeError {
        BridgeError::ProtobufDecodeError(e.into())
    }
}

impl From<serde_json::error::Error> for BridgeError {
    fn from(e: serde_json::error::Error) -> BridgeError {
        BridgeError::JsonDecodeError(e.into())
    }
}
