use super::validator::value::ValueParserError;
use super::attachment::*;

use std::str::FromStr;

#[derive(Debug, PartialEq, Clone)]
pub struct RelationInfo<Types: TypePack> { 
    pub to: String,
    pub to_field: String, 
    pub name: Option<String>, 
    pub on_delete: OnDeleteStrategy,
    pub attachment: Types::RelationAttachment
}

impl<Types: TypePack> RelationInfo<Types> {
    pub fn new(to: String, to_field: String) -> RelationInfo<Types> {
        RelationInfo {
            to: to,
            to_field: to_field,
            name: None,
            on_delete: OnDeleteStrategy::None,
            attachment: Types::RelationAttachment::default()
        }
    }
}

#[derive(Debug, Copy, PartialEq, Clone)]
pub enum OnDeleteStrategy {
    Cascade,
    None
}

impl FromStr for OnDeleteStrategy {
    type Err = ValueParserError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "CASCADE" => Ok(OnDeleteStrategy::Cascade),
            "NONE" => Ok(OnDeleteStrategy::None),
            _ => Err(ValueParserError::new(format!("Invalid onDelete strategy {}.", s)))
        }
    }
}