use super::validator::value::ValueParserError;
use super::traits::*;

use std::str::FromStr;

#[derive(Debug, Copy, PartialEq, Clone)]
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
            _ => Err(ValueParserError::new(format!("Invalid id strategy {}.", s))),
        }
    }
}

#[derive(Debug, Copy, PartialEq, Clone)]
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
            _ => Err(ValueParserError::new(format!("Invalid scalar list strategy {}.", s))),
        }
    }
}

#[derive(Debug, PartialEq, Clone)]
pub struct Sequence {
    pub name: String,
    pub initial_value: i32,
    pub allocation_size: i32,
}

impl WithName for Sequence {
    fn name(&self) -> &String {
        &self.name
    }
    fn set_name(&mut self, name: &String) {
        self.name = name.clone()
    }
}
