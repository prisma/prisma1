use super::traits::*;
use crate::ast;
use crate::errors::ValidationError;
use serde::{Deserialize, Serialize};

use crate::common::FromStrAndSpan;

/// Represents a strategy for generating IDs.
#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum IdStrategy {
    Auto,
    None,
}

impl FromStrAndSpan for IdStrategy {
    fn from_str_and_span(s: &str, span: &ast::Span) -> Result<Self, ValidationError> {
        match s {
            "AUTO" => Ok(IdStrategy::Auto),
            "NONE" => Ok(IdStrategy::None),
            _ => Err(ValidationError::new_literal_parser_error("id strategy", s, span)),
        }
    }
}

impl ToString for IdStrategy {
    fn to_string(&self) -> String {
        match self {
            IdStrategy::Auto => String::from("AUTO"),
            IdStrategy::None => String::from("NONE"),
        }
    }
}

/// Represents a sequence. Can be used to seed IDs.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Sequence {
    /// The name of the sequence.
    pub name: String,
    /// The initial value of the sequence.
    pub initial_value: i32,
    /// The allocation size of the sequence.
    pub allocation_size: i32,
}

impl WithName for Sequence {
    fn name(&self) -> &String {
        &self.name
    }
    fn set_name(&mut self, name: &str) {
        self.name = String::from(name)
    }
}
