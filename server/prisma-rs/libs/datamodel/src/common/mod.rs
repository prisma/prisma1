pub mod argument;
pub mod functions;
pub mod interpolation;
pub mod names;
pub mod value;

mod fromstr;
pub use fromstr::FromStrAndSpan;

use crate::ast;
use crate::errors::ValidationError;
use serde::{Deserialize, Serialize};

/// Prisma's builtin base types.
#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum PrismaType {
    Int,
    Float,
    Decimal,
    Boolean,
    String,
    DateTime,
    // This is similar to a context-dependent enum.
    ConstantLiteral
}

impl FromStrAndSpan for PrismaType {
    fn from_str_and_span(s: &str, span: &ast::Span) -> Result<Self, ValidationError> {
        match s {
            "ID" => Ok(PrismaType::Int),
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
            PrismaType::DateTime => String::from("DateTime"),
            PrismaType::ConstantLiteral => String::from("ConstantLiteral"),
        }
    }
}
