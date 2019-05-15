use crate::errors::LiteralParseError;
use serde::{Deserialize, Serialize};

use crate::ast;
use std::str::FromStr;

#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct RelationInfo {
    pub to: String,
    pub to_field: Option<String>,
    pub name: Option<String>,
    pub on_delete: OnDeleteStrategy,
}

impl RelationInfo {
    pub fn new(to: &str) -> RelationInfo {
        RelationInfo {
            to: String::from(to),
            to_field: None,
            name: None,
            on_delete: OnDeleteStrategy::None,
        }
    }
}

#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum OnDeleteStrategy {
    Cascade,
    None,
}

impl FromStr for OnDeleteStrategy {
    type Err = LiteralParseError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "CASCADE" => Ok(OnDeleteStrategy::Cascade),
            "NONE" => Ok(OnDeleteStrategy::None),
            _ => Err(LiteralParseError::new("onDelete strategy", s, &ast::Span::empty())),
        }
    }
}
