use super::traits::*;
use super::validator::value::ValueParserError;
use crate::ast;
use serde::{Deserialize, Serialize};

use std::str::FromStr;

#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum IdStrategy {
    Auto,
    None,
}

impl FromStr for IdStrategy {
    type Err = ValueParserError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "AUTO" => Ok(IdStrategy::Auto),
            "NONE" => Ok(IdStrategy::None),
            _ => Err(ValueParserError::new(
                &format!("Invalid id strategy {}.", s),
                s,
                &ast::Span::empty(),
            )),
        }
    }
}

#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum ScalarListStrategy {
    Embedded,
    Relation,
}

impl FromStr for ScalarListStrategy {
    type Err = ValueParserError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "EMBEDDED" => Ok(ScalarListStrategy::Embedded),
            "RELATION" => Ok(ScalarListStrategy::Relation),
            _ => Err(ValueParserError::new(
                &format!("Invalid scalar list strategy {}.", s),
                s,
                &ast::Span::empty(),
            )),
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
