use crate::ast;
use crate::dml::FromStrAndSpan;
use crate::errors::ValidationError;
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// Prisma's builtin base types.
#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum ScalarType {
    Int,
    Float,
    Decimal,
    Boolean,
    String,
    DateTime,
}

/// Value types for Prisma's builtin base types.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub enum Value {
    Int(i32),
    Float(f32),
    Decimal(f32),
    Boolean(bool),
    String(String),
    DateTime(DateTime<Utc>),
    ConstantLiteral(String),
}

impl FromStrAndSpan for ScalarType {
    fn from_str_and_span(s: &str, span: &ast::Span) -> Result<Self, ValidationError> {
        match s {
            "ID" => Ok(ScalarType::Int),
            "Int" => Ok(ScalarType::Int),
            "Float" => Ok(ScalarType::Float),
            "Decimal" => Ok(ScalarType::Decimal),
            "Boolean" => Ok(ScalarType::Boolean),
            "String" => Ok(ScalarType::String),
            "DateTime" => Ok(ScalarType::DateTime),
            _ => Err(ValidationError::new_type_not_found_error(s, span)),
        }
    }
}

/// Represents a strategy for embedding scalar lists.
#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum ScalarListStrategy {
    Embedded,
    Relation,
}

impl FromStrAndSpan for ScalarListStrategy {
    fn from_str_and_span(s: &str, span: &ast::Span) -> Result<Self, ValidationError> {
        match s {
            "EMBEDDED" => Ok(ScalarListStrategy::Embedded),
            "RELATION" => Ok(ScalarListStrategy::Relation),
            _ => Err(ValidationError::new_literal_parser_error("id strategy", s, span)),
        }
    }
}
