use super::traits::*;
use crate::ast;
use crate::errors::LiteralParseError;
use serde::{Deserialize, Serialize};

use std::str::FromStr;

#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum IdStrategy {
    Auto,
    None,
}

// TODO: Cannot use FromStr, since we need to propagate span

impl FromStr for IdStrategy {
    type Err = LiteralParseError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "AUTO" => Ok(IdStrategy::Auto),
            "NONE" => Ok(IdStrategy::None),
            _ => Err(LiteralParseError::new("id strategy", s, &ast::Span::empty())),
        }
    }
}

#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum ScalarListStrategy {
    Embedded,
    Relation,
}

impl FromStr for ScalarListStrategy {
    type Err = LiteralParseError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "EMBEDDED" => Ok(ScalarListStrategy::Embedded),
            "RELATION" => Ok(ScalarListStrategy::Relation),
            _ => Err(LiteralParseError::new("id strategy", s, &ast::Span::empty())),
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
