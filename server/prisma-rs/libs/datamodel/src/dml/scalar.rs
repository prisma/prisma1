use crate::ast;
use crate::common::FromStrAndSpan;
use crate::errors::ValidationError;
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use crate::common::PrismaType;

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
    Expression(String, PrismaType, Vec<Value>)
}

impl Value {
    fn get_type(&self) -> PrismaType {
        match self {
            Value::Int(_) => PrismaType::Int,
            Value::Float(_) => PrismaType::Float,
            Value::Decimal(_) => PrismaType::Decimal,
            Value::Boolean(_) => PrismaType::Boolean,
            Value::String(_) => PrismaType::String,
            Value::DateTime(_) => PrismaType::DateTime,
            Value::ConstantLiteral(_) => PrismaType::ConstantLiteral,
            Value::Expression(_, t, _) => *t
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
impl ToString for ScalarListStrategy {
    fn to_string(&self) -> String {
        match self {
            ScalarListStrategy::Embedded => String::from("EMBEDDED"),
            ScalarListStrategy::Relation => String::from("RELATION"),
        }
    }
}
