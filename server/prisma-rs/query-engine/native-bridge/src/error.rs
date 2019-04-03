use crate::protobuf;
use connector::{ConnectorError, NodeSelectorInfo};
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

impl From<NodeSelectorInfo> for protobuf::prisma::NodeSelector {
    fn from(info: NodeSelectorInfo) -> Self {
        Self {
            model_name: info.model,
            field_name: info.field,
            value: info.value.into(),
        }
    }
}

impl From<BridgeError> for protobuf::prisma::error::Value {
    fn from(error: BridgeError) -> protobuf::prisma::error::Value {
        match error {
            BridgeError::ConnectorError(e @ ConnectorError::ConnectionError(_)) => {
                protobuf::prisma::error::Value::ConnectionError(format!("{}", e))
            }

            BridgeError::ConnectorError(e @ ConnectorError::QueryError(_)) => {
                protobuf::prisma::error::Value::QueryError(format!("{}", e))
            }

            BridgeError::ConnectorError(e @ ConnectorError::InvalidConnectionArguments) => {
                protobuf::prisma::error::Value::QueryError(format!("{}", e))
            }

            BridgeError::ConnectorError(ConnectorError::FieldCannotBeNull { field }) => {
                protobuf::prisma::error::Value::FieldCannotBeNull(field)
            }

            BridgeError::ConnectorError(ConnectorError::UniqueConstraintViolation { field_name }) => {
                protobuf::prisma::error::Value::UniqueConstraintViolation(field_name)
            }

            BridgeError::ConnectorError(ConnectorError::RelationViolation {
                relation_name,
                model_a_name,
                model_b_name,
            }) => {
                let error = protobuf::prisma::RelationViolationError {
                    relation_name,
                    model_a_name,
                    model_b_name,
                };

                protobuf::prisma::error::Value::RelationViolation(error)
            }

            BridgeError::ConnectorError(ConnectorError::NodeNotFoundForWhere(info)) => {
                let node_selector = protobuf::prisma::NodeSelector {
                    model_name: info.model,
                    field_name: info.field,
                    value: info.value.into(),
                };

                protobuf::prisma::error::Value::NodeNotFoundForWhere(node_selector)
            }

            BridgeError::ConnectorError(ConnectorError::NodesNotConnected {
                relation_name,
                parent_name,
                parent_where,
                child_name,
                child_where,
            }) => {
                let error = protobuf::prisma::NodesNotConnectedError {
                    relation_name: relation_name,
                    parent_name: parent_name,
                    parent_where: parent_where.map(protobuf::prisma::NodeSelector::from),
                    child_name: child_name,
                    child_where: child_where.map(protobuf::prisma::NodeSelector::from),
                };

                protobuf::prisma::error::Value::NodesNotConnected(error)
            }

            e @ BridgeError::ProtobufDecodeError(_) => {
                protobuf::prisma::error::Value::ProtobufDecodeError(format!("{}", e))
            }

            e @ BridgeError::JsonDecodeError(_) => protobuf::prisma::error::Value::JsonDecodeError(format!("{}", e)),

            e @ BridgeError::DomainError(_) => protobuf::prisma::error::Value::InvalidInputError(format!("{}", e)),

            e => protobuf::prisma::error::Value::InvalidInputError(format!("{}", e)),
        }
    }
}
