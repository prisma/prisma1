//! Query builders module

mod query_builder;
mod read_new;

// --- to be removed / moved
mod write;
mod read;
pub use write::*;
pub use read::*;
// ---

pub use query_builder::*;
pub use read_new::*;

use crate::{QueryValidationError};
use prisma_models::{PrismaValue, ModelRef};
use std::{
    collections::BTreeMap,
    convert::TryInto
};
use connector::filter::RecordFinder;
use connector::QueryArguments;

/// Query builder sub-result type.
pub type QueryBuilderResult<T> = Result<T, QueryValidationError>;

/// Structures to represent parsed and validated parts of the query document,
/// used by the query builders.
pub struct ParsedObject {
    pub fields: Vec<ParsedField>
}

pub struct ParsedField {
    pub name: String,
    pub arguments: Vec<ParsedArgument>,
    pub sub_selections: Option<ParsedObject>,
}

impl ParsedField {
    /// Expects the caller to know that it is structurally guaranteed that a record finder can be extracted,
    /// e.g. that the query schema guarantees that the necessary fields are present.
    /// Errors occur if the arguments are structurally correct, but it's semantically impossible
    /// to extract a record finder, e.g. if too many fields are given.
    pub fn extract_record_finder(&self, model: &ModelRef) -> QueryBuilderResult<RecordFinder> {
        let where_arg = self.arguments.iter().find(|arg| arg.name == "where").unwrap();
        let values: BTreeMap<String, ParsedInputValue> = where_arg.value.clone().try_into().unwrap();

        if values.len() != 1 {
           Err(QueryValidationError::AssertionError(
               format!(
                   "Expected exactly one value for 'where' argument, got: {}",
                   values.iter().map(|v| v.0.as_str()).collect::<Vec<&str>>().join(", ")
               )
           ))
        } else {
            let field_selector: (String, ParsedInputValue) = values.into_iter().next().unwrap();
            let model_field = model.fields().find_from_scalar(&field_selector.0).unwrap();

            Ok(RecordFinder { field: model_field, value: field_selector.1.try_into().unwrap() })
        }
    }

    /// Expects the caller to know that it is structurally guaranteed that a query arguments can be extracted,
    /// e.g. that the query schema guarantees that the necessary fields are present.
    /// Errors occur if ...
    pub fn extract_query_args(&self) -> QueryBuilderResult<QueryArguments> {
        unimplemented!()
    }
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