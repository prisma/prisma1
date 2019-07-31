//! Parsed query document tree. Naming is WIP.
//! Structures represent parsed and validated parts of the query document, used by the query builders.

mod transformers;

pub use transformers::*;

use super::*;
use chrono::prelude::*;
use prisma_models::{EnumValue, EnumValueWrapper, GraphqlId, OrderBy, PrismaValue};
use serde_json::Value;
use std::{collections::BTreeMap, convert::TryInto};

pub type ParsedInputMap = BTreeMap<String, ParsedInputValue>;

#[derive(Debug, Clone)]
pub struct ParsedObject {
    pub fields: Vec<ParsedField>,
}

#[derive(Debug, Clone)]
pub struct ParsedField {
    pub name: String,
    pub alias: Option<String>,
    pub arguments: Vec<ParsedArgument>,
    pub sub_selections: Option<ParsedObject>,
}

#[derive(Debug, Clone)]
pub struct ParsedArgument {
    pub name: String,
    pub value: ParsedInputValue,
}

#[derive(Debug, Clone)]
pub enum ParsedInputValue {
    Single(PrismaValue),
    List(Vec<ParsedInputValue>),
    Map(ParsedInputMap),
}
