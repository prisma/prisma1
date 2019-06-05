pub mod argument;
pub mod functions;
pub mod interpolation;
pub mod names;
pub mod value;

mod fromstr;
pub use fromstr::FromStrAndSpan;

use crate::ast;
use crate::errors::ValidationError;
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// Prisma's builtin base types.
#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum PrismaType {
    Int,
    Float,
    Decimal,
    Boolean,
    String,
    DateTime
}

impl FromStrAndSpan for PrismaType {
    fn from_str_and_span(s: &str, span: &ast::Span) -> Result<Self, ValidationError> {
        match s {
            "Int" => Ok(PrismaType::Int),
            "Float" => Ok(PrismaType::Float),
            "Decimal" => Ok(PrismaType::Decimal),
            "Boolean" => Ok(PrismaType::Boolean),
            "String" => Ok(PrismaType::String),
            "DateTime" => Ok(PrismaType::DateTime),
            _ => Err(ValidationError::new_type_not_found_error(s, span)),
        }
    }
}

impl ToString for PrismaType {
    fn to_string(&self) -> String {
        match self {
            PrismaType::Int => String::from("Int"),
            PrismaType::Float => String::from("Float"),
            PrismaType::Decimal => String::from("Decimal"),
            PrismaType::Boolean => String::from("Boolean"),
            PrismaType::String => String::from("String"),
            PrismaType::DateTime => String::from("DateTime")
        }
    }
}

/// Value types for Prisma's builtin base types.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub enum PrismaValue {
    Int(i32),
    Float(f32),
    Decimal(f32),
    Boolean(bool),
    String(String),
    DateTime(DateTime<Utc>),
    ConstantLiteral(String),
    Expression(String, PrismaType, Vec<PrismaValue>),
}

impl PrismaValue {
    fn get_type(&self) -> PrismaType {
        match self {
            PrismaValue::Int(_) => PrismaType::Int,
            PrismaValue::Float(_) => PrismaType::Float,
            PrismaValue::Decimal(_) => PrismaType::Decimal,
            PrismaValue::Boolean(_) => PrismaType::Boolean,
            PrismaValue::String(_) => PrismaType::String,
            PrismaValue::DateTime(_) => PrismaType::DateTime,
            PrismaValue::Expression(_, t, _) => *t,
            PrismaValue::ConstantLiteral(_) => panic!("Constant literal values do not map to a base type and should never surface.")
        }
    }
}

impl ToString for PrismaValue {
    fn to_string(&self) -> String {
        match self {
            PrismaValue::Int(val) => format!("{}", val),
            PrismaValue::Float(val) => format!("{}", val),
            PrismaValue::Decimal(val) => format!("{}", val),
            PrismaValue::Boolean(val) => format!("{}", val),
            PrismaValue::String(val) => format!("{}", val),
            PrismaValue::DateTime(val) => format!("{}", val),
            PrismaValue::ConstantLiteral(val) => format!("{}", val),
            PrismaValue::Expression(_, t, _) => format!("Function<{}>", t.to_string()),
        }
    }
}
