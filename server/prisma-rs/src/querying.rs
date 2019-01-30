use crate::schema::{Field, Model};

use rusqlite::{
    types::{Null, ToSql, ToSqlOutput},
    Error as RusqlError,
};

use chrono::{DateTime, Utc};

#[derive(Debug, PartialEq)]
pub enum PrismaValue {
    String(String),
    Float(f64),
    Boolean(bool),
    Null,
    DateTime(DateTime<Utc>),
    Int(i64),
    Enum(String),
    Json(String),
    GraphQLID(String),
    Uuid(String),
    Relation(u64),
}

impl ToSql for PrismaValue {
    fn to_sql(&self) -> Result<ToSqlOutput, RusqlError> {
        let value = match self {
            PrismaValue::String(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Enum(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Json(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Uuid(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::GraphQLID(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Float(value) => ToSqlOutput::from(*value),
            PrismaValue::Int(value) => ToSqlOutput::from(*value),
            PrismaValue::Relation(value) => ToSqlOutput::from(*value as i64),
            PrismaValue::Boolean(value) => ToSqlOutput::from(*value),
            PrismaValue::DateTime(value) => value.to_sql().unwrap(),
            PrismaValue::Null => ToSqlOutput::from(Null),
        };

        Ok(value)
    }
}

/// A helper struct for selecting data.
pub struct NodeSelector<'a> {
    /// The model to select from
    pub model: &'a Model,
    /// The table to look into
    pub table: String,
    /// The name of the field to filtering
    pub field: &'a Field,
    /// The value of the field, should be in the corresponding type.
    pub value: &'a PrismaValue,
    /// Fields to select from the table
    pub selected_fields: &'a [Field],
}

impl<'a> NodeSelector<'a> {
    pub fn new(
        database: &'a str,
        model: &'a Model,
        field: &'a Field,
        value: &'a PrismaValue,
        selected_fields: &'a [Field],
    ) -> NodeSelector<'a> {
        let table = format!("{}.{}", database, model.stable_identifier);

        NodeSelector {
            model,
            table,
            field,
            value,
            selected_fields,
        }
    }
}
