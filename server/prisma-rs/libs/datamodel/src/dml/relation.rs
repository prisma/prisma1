use crate::errors::ValidationError;
use serde::{Deserialize, Serialize};

use super::FromStrAndSpan;
use crate::ast;

/// Holds information about a relation field.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct RelationInfo {
    /// The target model of the relation.
    pub to: String,
    /// The target field of the relation.
    pub to_fields: Vec<String>,
    /// The name of the relation.
    pub name: Option<String>,
    /// A strategy indicating what happens when
    /// a related node is deleted.
    pub on_delete: OnDeleteStrategy,
}

impl RelationInfo {
    /// Creates a new relation info for the
    /// given target model.
    pub fn new(to: &str) -> RelationInfo {
        RelationInfo {
            to: String::from(to),
            to_fields: Vec::new(),
            name: None,
            on_delete: OnDeleteStrategy::None,
        }
    }
    pub fn new_with_field(to: &str, to_field: &str) -> RelationInfo {
        RelationInfo {
            to: String::from(to),
            to_fields: vec![String::from(to_field)],
            name: None,
            on_delete: OnDeleteStrategy::None,
        }
    }

    pub fn new_with_fields(to: &str, to_fields: Vec<String>) -> RelationInfo {
        RelationInfo {
            to: String::from(to),
            to_fields: to_fields,
            name: None,
            on_delete: OnDeleteStrategy::None,
        }
    }
}

/// Describes what happens when related nodes
/// are deleted.
#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum OnDeleteStrategy {
    Cascade,
    None,
}

impl FromStrAndSpan for OnDeleteStrategy {
    fn from_str_and_span(s: &str, span: &ast::Span) -> Result<Self, ValidationError> {
        match s {
            "CASCADE" => Ok(OnDeleteStrategy::Cascade),
            "NONE" => Ok(OnDeleteStrategy::None),
            _ => Err(ValidationError::new_literal_parser_error("onDelete strategy", s, span)),
        }
    }
}
