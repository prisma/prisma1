use crate::{CoreError, CoreResult};
use connector::{filter::Filter, RelationCompare, ScalarCompare};
use graphql_parser::query::Value;
use prisma_models::{Field, ModelRef, PrismaValue};
use std::{collections::BTreeMap, sync::Arc};

#[derive(PartialEq)]
enum FilterOp {
    In,
    NotIn,
    Not,
    Lt,
    Lte,
    Gt,
    Gte,
    Contains,
    NotContains,
    StartsWith,
    NotStartsWith,
    EndsWith,
    NotEndsWith,
    Some,
    None,
    Every,
    NestedAnd,
    NestedOr,
    NestedNot,
    Field,
}

impl From<&FilterOp> for &'static str {
    fn from(fo: &FilterOp) -> Self {
        match fo {
            FilterOp::In => "_in",
            FilterOp::NotIn => "_not_in",
            FilterOp::Not => "_not",
            FilterOp::Lt => "_lt",
            FilterOp::Lte => "_lte",
            FilterOp::Gt => "_gt",
            FilterOp::Gte => "_gte",
            FilterOp::Contains => "_contains",
            FilterOp::NotContains => "_not_contains",
            FilterOp::StartsWith => "_starts_with",
            FilterOp::NotStartsWith => "_not_starts_with",
            FilterOp::EndsWith => "_ends_with",
            FilterOp::NotEndsWith => "_not_ends_with",
            FilterOp::Some => "_some",
            FilterOp::None => "_none",
            FilterOp::Every => "_every",
            FilterOp::NestedAnd => "AND",
            FilterOp::NestedOr => "OR",
            FilterOp::NestedNot => "NOT",
            FilterOp::Field => "", // Needs to be last
        }
    }
}

fn to_prisma_value(v: &Value) -> PrismaValue {
    match v {
        Value::Boolean(b) => PrismaValue::Boolean(b.clone()),
        Value::Enum(e) => PrismaValue::Enum(e.clone()),
        Value::Float(f) => PrismaValue::Float(f.clone()),
        Value::Int(i) => PrismaValue::Int(i.as_i64().unwrap() as i32),
        Value::Null => PrismaValue::Null,
        Value::String(s) => PrismaValue::String(s.clone()),
        Value::List(l) => PrismaValue::List(Some(l.iter().map(|i| to_prisma_value(i)).collect())),
        _ => unimplemented!(),
    }
}

pub fn extract_filter(map: &BTreeMap<String, Value>, model: ModelRef) -> CoreResult<Filter> {
    let ops = vec![
        FilterOp::NotIn,
        FilterOp::NotContains,
        FilterOp::NotStartsWith,
        FilterOp::NotEndsWith,
        FilterOp::In,
        FilterOp::Not,
        FilterOp::Lt,
        FilterOp::Lte,
        FilterOp::Gt,
        FilterOp::Gte,
        FilterOp::Contains,
        FilterOp::StartsWith,
        FilterOp::EndsWith,
        FilterOp::Some,
        FilterOp::None,
        FilterOp::Every,
        FilterOp::NestedAnd,
        FilterOp::NestedOr,
        FilterOp::NestedNot,
        FilterOp::Field, // Needs to be last
    ];

    let filters = map
        .iter()
        .map(|(k, v): (&String, &Value)| {
            let op = ops.iter().find(|op| {
                let op_name: &'static str = (*op).into();
                k.as_str().ends_with(op_name)
            });

            let op = match op {
                None => return Err(CoreError::QueryValidationError(format!("Query argument {} invalid", k))),
                Some(op) => op,
            };

            match op {
                op if (op == &FilterOp::NestedAnd || op == &FilterOp::NestedOr || op == &FilterOp::NestedNot) => {
                    let extract_nested = |v: &Value| {
                        extract_filter(
                            match v {
                                Value::Object(o) => o,
                                _ => panic!("Expected object value"),
                            },
                            Arc::clone(&model),
                        )
                        .unwrap()
                    };

                    let value: Vec<Filter> = match v {
                        Value::List(l) => l.into_iter().map(extract_nested).collect(),
                        Value::Object(_) => vec![extract_nested(v)],
                        _ => unreachable!(),
                    };

                    Ok(match op {
                        FilterOp::NestedAnd => Filter::and(value),
                        FilterOp::NestedOr => Filter::or(value),
                        FilterOp::NestedNot => Filter::not(value),
                        _ => unreachable!(),
                    })
                }
                op => {
                    let op_name: &'static str = op.into();
                    let field_name = k.trim_end_matches(op_name);
                    let field = model.fields().find_from_all(&field_name).unwrap(); // fixme: unwrap

                    match field {
                        Field::Scalar(s) => {
                            let value = to_prisma_value(v);
                            Ok(match op {
                                FilterOp::In => s.is_in(value.into()),
                                FilterOp::NotIn => s.not_in(value.into()),
                                FilterOp::Not => s.not_equals(value),
                                FilterOp::Lt => s.less_than(value),
                                FilterOp::Lte => s.less_than_or_equals(value),
                                FilterOp::Gt => s.greater_than(value),
                                FilterOp::Gte => s.greater_than_or_equals(value),
                                FilterOp::Contains => s.contains(value),
                                FilterOp::NotContains => s.not_contains(value),
                                FilterOp::StartsWith => s.starts_with(value),
                                FilterOp::NotStartsWith => s.not_starts_with(value),
                                FilterOp::EndsWith => s.ends_with(value),
                                FilterOp::NotEndsWith => s.not_ends_with(value),
                                FilterOp::Field => s.equals(value),
                                _ => unreachable!(),
                            })
                        }
                        Field::Relation(r) => {
                            let value = match v {
                                Value::Object(o) => o,
                                _ => panic!("Expected object value"),
                            };

                            Ok(match op {
                                FilterOp::Some => r.at_least_one_related(extract_filter(value, r.related_model())?),
                                FilterOp::None => r.no_related(extract_filter(value, r.related_model())?),
                                FilterOp::Every => r.every_related(extract_filter(value, r.related_model())?),
                                FilterOp::Field => r.to_one_related(extract_filter(value, r.related_model())?),
                                _ => unreachable!(),
                            })
                        }
                    }
                }
            }
        })
        .collect::<CoreResult<Vec<Filter>>>()?;

    Ok(Filter::and(filters))
}
