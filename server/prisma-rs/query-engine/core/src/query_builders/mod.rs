//! Query builders module

mod query_builder;
mod read_new;
mod utils;

// --- to be removed / moved
//mod write;
//mod read;
//pub use write::*;
//pub use read::*;
// ---

pub use query_builder::*;
pub use read_new::*;
pub use utils::*;

use crate::{QueryValidationError};
use prisma_models::{PrismaValue, ModelRef};
use std::{
    collections::BTreeMap,
    convert::TryInto
};

/// Query builder sub-result type.
pub type QueryBuilderResult<T> = Result<T, QueryValidationError>;

/// Structures to represent parsed and validated parts of the query document,
/// used by the query builders.
pub struct ParsedObject {
    pub fields: Vec<ParsedField>
}

pub struct ParsedField {
    pub name: String,
    pub alias: Option<String>,
    pub arguments: Vec<ParsedArgument>,
    pub sub_selections: Option<ParsedObject>,
}

pub struct ParsedArgument {
    pub name: String,
    pub value: ParsedInputValue,
}

#[derive(Clone)]
pub enum ParsedInputValue {
    Single(PrismaValue),
    Map(BTreeMap<String, ParsedInputValue>),
}

impl TryInto<PrismaValue> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> Result<PrismaValue, Self::Error> {
        match self {
            ParsedInputValue::Single(val) => Ok(val),
            _ => Err(QueryValidationError::AssertionError("Attempted conversion of non-single (map) ParsedInputValue into PrismaValue.".into()))
        }
    }
}

impl TryInto<BTreeMap<String, ParsedInputValue>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> Result<BTreeMap<String, ParsedInputValue>, Self::Error> {
        match self {
            ParsedInputValue::Map(val) => Ok(val),
            _ => Err(QueryValidationError::AssertionError("Attempted conversion of single ParsedInputValue into map.".into()))
        }
    }
}