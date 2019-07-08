use super::*;
use crate::{
    query_builders::{ParsedArgument, ParsedInputValue, QueryBuilderResult},
    QueryValidationError,
};
use connector::filter::RecordFinder;
use connector::QueryArguments;
use prisma_models::ModelRef;

/// Expects the caller to know that it is structurally guaranteed that a record finder can be extracted
/// from the given set of arguments, e.g. that the query schema guarantees that the necessary fields are present.
/// Errors occur if the arguments are structurally correct, but it's semantically impossible
/// to extract a record finder, e.g. if too many fields are given.
pub fn extract_record_finder(arguments: Vec<ParsedArgument>, model: &ModelRef) -> QueryBuilderResult<RecordFinder> {
    let where_arg = arguments.into_iter().find(|arg| arg.name == "where").unwrap();
    let values: BTreeMap<String, ParsedInputValue> = where_arg.value.try_into().unwrap();

    if values.len() != 1 {
        Err(QueryValidationError::AssertionError(format!(
            "Expected exactly one value for 'where' argument, got: {}",
            values.iter().map(|v| v.0.as_str()).collect::<Vec<&str>>().join(", ")
        )))
    } else {
        let field_selector: (String, ParsedInputValue) = values.into_iter().next().unwrap();
        let model_field = model.fields().find_from_scalar(&field_selector.0).unwrap();

        Ok(RecordFinder {
            field: model_field,
            value: field_selector.1.try_into().unwrap(),
        })
    }
}

/// Expects the caller to know that it is structurally guaranteed that query arguments can be extracted,
/// e.g. that the query schema guarantees that required fields are present.
/// Errors occur if ...
pub fn extract_query_args(arguments: Vec<ParsedArgument>, model: &ModelRef) -> QueryBuilderResult<QueryArguments> {
    // arguments.into_iter()
    //     .fold(Ok(QueryArguments::default()), |result, arg| {
    //         if let Ok(res) = result {
    //             match arg.name.as_str() {
    //                 "skip" => Ok(QueryArguments { skip: Some(arg.value u32), ..res }),
    //                 // ("first", Value::Int(num)) => match num.as_i64() {
    //                 //     Some(num) => Ok(QueryArguments { first: Some(num as u32), ..res }),
    //                 //     None => Err(CoreError::LegacyQueryValidationError("Invalid number provided".into())),
    //                 // },
    //                 // ("last", Value::Int(num)) => match num.as_i64() {
    //                 //     Some(num) => Ok(QueryArguments { last: Some(num as u32), ..res }),
    //                 //     None => Err(CoreError::LegacyQueryValidationError("Invalid number provided".into())),
    //                 // },
    //                 // ("after", Value::String(s)) if s.is_uuid() => Ok(QueryArguments { after: Some(GraphqlId::UUID(s.as_uuid())), ..res }),
    //                 // ("after", Value::String(s)) => Ok(QueryArguments { after: Some(s.clone().into()), ..res }),
    //                 // ("after", Value::Int(num)) => match num.as_i64() {
    //                 //     Some(num) => Ok(QueryArguments { after: Some((num as usize).into()), ..res }),
    //                 //     None => Err(CoreError::LegacyQueryValidationError("Invalid number provided".into())),
    //                 // },
    //                 // ("before", Value::String(s)) if s.is_uuid() => Ok(QueryArguments { before: Some(GraphqlId::UUID(s.as_uuid())), ..res }),
    //                 // ("before", Value::String(s)) => Ok(QueryArguments { before: Some(s.clone().into()), ..res }),
    //                 // ("before", Value::Int(num)) => match num.as_i64() {
    //                 //     Some(num) => Ok(QueryArguments { after: Some((num as usize).into()), ..res }),
    //                 //     None => Err(CoreError::LegacyQueryValidationError("Invalid number provided".into())),
    //                 // },
    //                 // ("orderby", Value::Enum(order_arg)) => extract_order_by(res, order_arg, Arc::clone(&model)),
    //                 // ("where", Value::Object(o)) => extract_filter(res, o, Arc::clone(&model)),
    //                 _ => result,
    //             }
    //         } else {
    //             result
    //         }
    //     })

    unimplemented!()
}
