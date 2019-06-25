use crate::{
    error::{PrismaError, PrismaError::QueryParsingError},
    PrismaResult,
};
use core::query_ir::*;
use core::Query;
use graphql_parser::query::{
    Definition, Document, OperationDefinition, Selection as GqlSelection, SelectionSet, Value,
};
use std::collections::BTreeMap;

/// Protocol adapter for GraphQL -> Query Document
pub struct GraphQLProtocolAdapter;

impl GraphQLProtocolAdapter {
    pub fn convert(gql_doc: Document, operation: Option<String>) -> PrismaResult<QueryDocument> {
        let operations: Vec<Operation> = match operation {
            Some(ref op) => gql_doc
                .definitions
                .into_iter()
                .find(|def| Self::matches_operation(def, op))
                .ok_or(QueryParsingError(format!(
                    "Operation '{}' does not match any query.",
                    op
                )))
                .and_then(|def| Self::convert_definition(def).map(|r| vec![r])),

            None => gql_doc
                .definitions
                .into_iter()
                .map(|def| Self::convert_definition(def))
                .collect::<PrismaResult<Vec<Operation>>>(),
        }?;

        Ok(QueryDocument { operations })
    }

    fn convert_definition(def: Definition) -> PrismaResult<Operation> {
        match def {
            Definition::Fragment(f) => Err(PrismaError::UnsupportedFeatureError(
                "Fragment definition",
                format!("Fragment '{}', at position {}.", f.name, f.position),
            )),
            Definition::Operation(op) => match op {
                OperationDefinition::Subscription(s) => Err(PrismaError::UnsupportedFeatureError(
                    "Subscription query",
                    format!("At position {}.", s.position),
                )),
                OperationDefinition::SelectionSet(s) => Self::convert_query(None, s),
                OperationDefinition::Query(q) => Self::convert_query(q.name, q.selection_set),
                OperationDefinition::Mutation(m) => unimplemented!(),
            },
        }
    }

    fn convert_query(name: Option<String>, selection_set: SelectionSet) -> PrismaResult<Operation> {
        Self::convert_selection_set(selection_set).map(|selections| Operation::Read(ReadOperation { name, selections }))
    }

    fn convert_mutation(name: Option<String>, selection_set: SelectionSet) -> PrismaResult<Operation> {
        Self::convert_selection_set(selection_set)
            .map(|selections| Operation::Write(WriteOperation { name, selections }))
    }

    fn convert_selection_set(selection_set: SelectionSet) -> PrismaResult<Vec<Selection>> {
        selection_set
            .items
            .into_iter()
            .map(|item| match item {
                GqlSelection::Field(f) => {
                    let arguments: Vec<(String, QueryValue)> = f
                        .arguments
                        .into_iter()
                        .map(|(k, v)| Ok((k, Self::convert_value(v)?)))
                        .collect::<PrismaResult<Vec<_>>>()?;

                    Ok(Selection {
                        name: f.name,
                        alias: f.alias,
                        arguments,
                        sub_selections: Self::convert_selection_set(f.selection_set)?,
                    })
                }
                GqlSelection::FragmentSpread(fs) => Err(PrismaError::UnsupportedFeatureError(
                    "Fragment spread",
                    format!("Fragment '{}', at position {}.", fs.fragment_name, fs.position),
                )),
                GqlSelection::InlineFragment(i) => Err(PrismaError::UnsupportedFeatureError(
                    "Inline fragment",
                    format!("At position {}.", i.position),
                )),
            })
            .collect()
    }

    /// Checks if the given GraphQL definition matches the operation name that should be executed.
    fn matches_operation(def: &Definition, operation: &str) -> bool {
        let check = |n: Option<&String>| n.filter(|name| name.as_str() == operation).is_some();
        match def {
            Definition::Fragment(f) => false,
            Definition::Operation(op) => match op {
                OperationDefinition::Subscription(s) => check(s.name.as_ref()),
                OperationDefinition::SelectionSet(s) => false,
                OperationDefinition::Query(q) => check(q.name.as_ref()),
                OperationDefinition::Mutation(m) => check(m.name.as_ref()),
            },
        }
    }

    fn convert_value(value: Value) -> PrismaResult<QueryValue> {
        match value {
            Value::Variable(name) => Err(PrismaError::UnsupportedFeatureError(
                "Variable usage",
                format!("Variable '{}'.", name),
            )),
            Value::Int(i) => match i.as_i64() {
                Some(i) => Ok(QueryValue::Int(i)),
                None => Err(PrismaError::QueryValidationError(format!(
                    "Invalid 64 bit integer: {:?}",
                    i
                ))),
            },
            Value::Float(f) => Ok(QueryValue::Float(f)),
            Value::String(s) => Ok(QueryValue::String(s)),
            Value::Boolean(b) => Ok(QueryValue::Boolean(b)),
            Value::Null => Ok(QueryValue::Null),
            Value::Enum(e) => Ok(QueryValue::Enum(e)),
            Value::List(values) => {
                let values: Vec<QueryValue> = values
                    .into_iter()
                    .map(|v| Self::convert_value(v))
                    .collect::<PrismaResult<Vec<QueryValue>>>()?;

                Ok(QueryValue::List(values))
            }
            Value::Object(map) => {
                let values = map
                    .into_iter()
                    .map(|(k, v)| Self::convert_value(v).map(|v| (k, v)))
                    .collect::<PrismaResult<BTreeMap<String, QueryValue>>>()?;

                Ok(QueryValue::Object(values))
            }
        }
    }
}
