use crate::{
    query_builders::{ParsedArgument, QueryBuilderResult, ParsedInputValue},
    QueryValidationError
};
use connector::filter::RecordFinder;
use prisma_models::ModelRef;
use connector::QueryArguments;
use super::*;

/// Expects the caller to know that it is structurally guaranteed that a record finder can be extracted
/// from the given set of arguments, e.g. that the query schema guarantees that the necessary fields are present.
/// Errors occur if the arguments are structurally correct, but it's semantically impossible
/// to extract a record finder, e.g. if too many fields are given.
pub fn extract_record_finder(arguments: Vec<ParsedArgument>, model: &ModelRef) -> QueryBuilderResult<RecordFinder> {
    let where_arg = arguments.into_iter().find(|arg| arg.name == "where").unwrap();
    let values: BTreeMap<String, ParsedInputValue> = where_arg.value.try_into().unwrap();

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
pub fn extract_query_args(arguments: Vec<ParsedArgument>, model: &ModelRef) -> QueryBuilderResult<QueryArguments> {
    unimplemented!()
}