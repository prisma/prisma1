//! Query builders module

mod query_builder;
mod read_new;
mod utils;
mod write_new;

// --- to be removed / moved
//mod write;
//mod read;
//pub use write::*;
//pub use read::*;
// ---

pub use query_builder::*;
pub use read_new::*;
pub use write_new::*;
pub use utils::*;

use crate::QueryValidationError;
use chrono::prelude::*;
use prisma_models::{GraphqlId, PrismaValue};
use serde_json::Value;
use std::{collections::BTreeMap, convert::TryInto};

/// Query builder sub-result type.
pub type QueryBuilderResult<T> = Result<T, QueryValidationError>;

/// Structures to represent parsed and validated parts of the query document,
/// used by the query builders.
pub struct ParsedObject {
    pub fields: Vec<ParsedField>,
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

impl ParsedInputValue {
    pub fn to_map(self) -> Result<BTreeMap<String, ParsedInputValue>, QueryValidationError> {
        self.try_into()
    }
}

impl TryInto<PrismaValue> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<PrismaValue> {
        match self {
            ParsedInputValue::Single(val) => Ok(val),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of non-single (map) ParsedInputValue into PrismaValue.".into(),
            )),
        }
    }
}

impl TryInto<BTreeMap<String, ParsedInputValue>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<BTreeMap<String, ParsedInputValue>> {
        match self {
            ParsedInputValue::Map(val) => Ok(val),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of single ParsedInputValue into map.".into(),
            )),
        }
    }
}

// --- Convenience transformers for direct unpacking ---

impl TryInto<Option<String>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<Option<String>> {
        let prisma_value: PrismaValue = self.try_into()?;

        match prisma_value {
            PrismaValue::String(s) => Ok(Some(s)),
            PrismaValue::Enum(s) => Ok(Some(s)),
            PrismaValue::Null => Ok(None),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of non-String Prisma value type into String failed.".into(),
            )),
        }
    }
}

impl TryInto<Option<f64>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<Option<f64>> {
        let prisma_value: PrismaValue = self.try_into()?;

        match prisma_value {
            PrismaValue::Float(f) => Ok(Some(f)),
            PrismaValue::Null => Ok(None),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of non-float Prisma value type into float failed.".into(),
            )),
        }
    }
}

impl TryInto<Option<bool>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<Option<bool>> {
        let prisma_value: PrismaValue = self.try_into()?;

        match prisma_value {
            PrismaValue::Boolean(b) => Ok(Some(b)),
            PrismaValue::Null => Ok(None),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of non-bool Prisma value type into bool failed.".into(),
            )),
        }
    }
}

impl TryInto<Option<DateTime<Utc>>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<Option<DateTime<Utc>>> {
        let prisma_value: PrismaValue = self.try_into()?;

        match prisma_value {
            PrismaValue::DateTime(dt) => Ok(Some(dt)),
            PrismaValue::Null => Ok(None),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of non-DateTime Prisma value type into DateTime failed.".into(),
            )),
        }
    }
}

impl TryInto<Option<Value>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<Option<Value>> {
        let prisma_value: PrismaValue = self.try_into()?;

        match prisma_value {
            PrismaValue::Json(j) => Ok(Some(j)),
            PrismaValue::Null => Ok(None),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of non-JSON Prisma value type into JSON failed.".into(),
            )),
        }
    }
}

impl TryInto<Option<i64>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<Option<i64>> {
        let prisma_value: PrismaValue = self.try_into()?;

        match prisma_value {
            PrismaValue::Int(i) => Ok(Some(i)),
            PrismaValue::Null => Ok(None),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of non-int Prisma value type into int failed.".into(),
            )),
        }
    }
}

impl TryInto<Option<GraphqlId>> for ParsedInputValue {
    type Error = QueryValidationError;

    fn try_into(self) -> QueryBuilderResult<Option<GraphqlId>> {
        let prisma_value: PrismaValue = self.try_into()?;

        match prisma_value {
            PrismaValue::GraphqlId(id) => Ok(Some(id)),
            PrismaValue::Null => Ok(None),
            _ => Err(QueryValidationError::AssertionError(
                "Attempted conversion of non-id Prisma value type into id failed.".into(),
            )),
        }
    }
}
