use crate::filter::NodeSelector;
use failure::{Error, Fail};
use prisma_models::prelude::{DomainError, GraphqlId, ModelRef, PrismaValue};
use std::fmt;

#[cfg(feature = "sqlite")]
use rusqlite;

#[cfg(feature = "sqlite")]
use libsqlite3_sys as ffi;

#[derive(Debug)]
pub struct NodeSelectorInfo {
    pub model: String,
    pub field: String,
    pub value: PrismaValue,
}

impl NodeSelectorInfo {
    pub fn for_id(model: ModelRef, value: &GraphqlId) -> Self {
        Self {
            model: model.name.clone(),
            field: model.fields().id().name.clone(),
            value: PrismaValue::from(value.clone()),
        }
    }
}

impl fmt::Display for NodeSelectorInfo {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(
            f,
            "field {} in model {} with value {}",
            self.model, self.field, self.value
        )
    }
}

impl From<&NodeSelector> for NodeSelectorInfo {
    fn from(ns: &NodeSelector) -> Self {
        Self {
            model: ns.field.model().name.clone(),
            field: ns.field.name.clone(),
            value: ns.value.clone(),
        }
    }
}

#[derive(Debug, Fail)]
pub enum ConnectorError {
    #[fail(display = "Unique constraint failed: {}", field_name)]
    UniqueConstraintViolation { field_name: String },

    #[fail(display = "Node does not exist.")]
    NodeDoesNotExist,

    #[fail(display = "Column does not exist")]
    ColumnDoesNotExist,

    #[fail(display = "Error creating a database connection.")]
    ConnectionError(Error),

    #[fail(display = "Error querying the database: {}", _0)]
    QueryError(Error),

    #[fail(display = "The provided arguments are not supported.")]
    InvalidConnectionArguments,

    #[fail(display = "The column value was different from the model")]
    ColumnReadFailure(Error),

    #[fail(display = "Field cannot be null: {}", field)]
    FieldCannotBeNull { field: String },

    #[fail(display = "{}", _0)]
    DomainError(DomainError),

    #[fail(display = "Node not found: {}", _0)]
    NodeNotFoundForWhere(NodeSelectorInfo),

    #[fail(
        display = "Violating a relation {} between {} and {}",
        relation_name, model_a_name, model_b_name
    )]
    RelationViolation {
        relation_name: String,
        model_a_name: String,
        model_b_name: String,
    },

    #[fail(
        display = "The relation {} has no node for the model {} connected to a Node for the model {} on your mutation path.",
        relation_name, parent_name, child_name
    )]
    NodesNotConnected {
        relation_name: String,
        parent_name: String,
        parent_where: Option<NodeSelectorInfo>,
        child_name: String,
        child_where: Option<NodeSelectorInfo>,
    },

    #[fail(display = "Conversion error: {}", _0)]
    ConversionError(Error),
}

impl From<DomainError> for ConnectorError {
    fn from(e: DomainError) -> ConnectorError {
        ConnectorError::DomainError(e)
    }
}

impl From<serde_json::error::Error> for ConnectorError {
    fn from(e: serde_json::error::Error) -> ConnectorError {
        ConnectorError::ConversionError(e.into())
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
