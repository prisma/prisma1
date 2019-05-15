use super::traits::*;
use crate::ast;
use crate::errors::LiteralParseError;
use serde::{Deserialize, Serialize};

use super::FromStrAndSpan;

#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum IdStrategy {
    Auto,
    None,
}

// TODO: Cannot use FromStr, since we need to propagate span

impl FromStrAndSpan for IdStrategy {
    fn from_str_and_span(s: &str, span: &ast::Span) -> Result<Self, LiteralParseError> {
        match s {
            "AUTO" => Ok(IdStrategy::Auto),
            "NONE" => Ok(IdStrategy::None),
            _ => Err(LiteralParseError::new("id strategy", s, span)),
        }
    }
}

#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum ScalarListStrategy {
    Embedded,
    Relation,
}

impl FromStrAndSpan for ScalarListStrategy {
    fn from_str_and_span(s: &str, span: &ast::Span) -> Result<Self, LiteralParseError> {
        match s {
            "EMBEDDED" => Ok(ScalarListStrategy::Embedded),
            "RELATION" => Ok(ScalarListStrategy::Relation),
            _ => Err(LiteralParseError::new("id strategy", s, span)),
        }
    }
}

#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Sequence {
    pub name: String,
    pub initial_value: i32,
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
