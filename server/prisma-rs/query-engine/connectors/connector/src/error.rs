use crate::filter::RecordFinder;
use failure::{Error, Fail};
use prisma_models::prelude::{DomainError, GraphqlId, ModelRef, PrismaValue};
use std::fmt;

#[derive(Debug)]
pub struct RecordFinderInfo {
    pub model: String,
    pub field: String,
    pub value: PrismaValue,
}

impl RecordFinderInfo {
    pub fn for_id(model: ModelRef, value: &GraphqlId) -> Self {
        Self {
            model: model.name.clone(),
            field: model.fields().id().name.clone(),
            value: PrismaValue::from(value.clone()),
        }
    }
}

impl fmt::Display for RecordFinderInfo {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(
            f,
            "field {} in model {} with value {}",
            self.model, self.field, self.value
        )
    }
}

impl From<&RecordFinder> for RecordFinderInfo {
    fn from(ns: &RecordFinder) -> Self {
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

    #[fail(display = "Null constraint failed: {}", field_name)]
    NullConstraintViolation { field_name: String },

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
    NodeNotFoundForWhere(RecordFinderInfo),

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
        parent_where: Option<RecordFinderInfo>,
        child_name: String,
        child_where: Option<RecordFinderInfo>,
    },

    #[fail(display = "Conversion error: {}", _0)]
    ConversionError(Error),

    #[fail(display = "Database creation error: {}", _0)]
    DatabaseCreationError(&'static str),
}

impl From<DomainError> for ConnectorError {
    fn from(e: DomainError) -> ConnectorError {
        ConnectorError::DomainError(e)
    }
}
