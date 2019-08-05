use crate::query_builders::{ParsedInputValue, QueryBuilderResult};
use connector::{filter::Filter, RelationCompare, ScalarCompare};
use prisma_models::{Field, ModelRef, PrismaListValue, PrismaValue};
use std::{collections::BTreeMap, convert::TryFrom, convert::TryInto};

lazy_static! {
    /// Filter operations in descending order of how they should be checked.
    static ref FILTER_OPERATIONS: Vec<FilterOp> = vec![
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
}

#[derive(Debug, PartialEq, Clone, Copy)]
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

impl FilterOp {
    pub fn find_op(name: &str) -> Option<FilterOp> {
        FILTER_OPERATIONS
            .iter()
            .find(|op| {
                let op_suffix: &'static str = op.suffix();
                name.ends_with(op_suffix)
            })
            .map(|op| op.clone())
    }

    pub fn suffix(&self) -> &'static str {
        match self {
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
            FilterOp::Field => "",
        }
    }
}

pub fn extract_filter(value_map: BTreeMap<String, ParsedInputValue>, model: &ModelRef) -> QueryBuilderResult<Filter> {
    let filters = value_map
        .into_iter()
        .map(|(key, value): (String, ParsedInputValue)| {
            let op = FilterOp::find_op(key.as_str()).unwrap();

            match op {
                op if (op == FilterOp::NestedAnd || op == FilterOp::NestedOr || op == FilterOp::NestedNot) => {
                    let value: QueryBuilderResult<Vec<Filter>> = match value {
                        ParsedInputValue::List(values) => values
                            .into_iter()
                            .map(|val| extract_filter(val.try_into()?, model))
                            .collect(),

                        ParsedInputValue::Map(map) => extract_filter(map, model).map(|res| vec![res]),
                        _ => unreachable!(),
                    };

                    value.map(|value| match op {
                        FilterOp::NestedAnd => Filter::and(value),
                        FilterOp::NestedOr => Filter::or(value),
                        FilterOp::NestedNot => Filter::not(value),
                        _ => unreachable!(),
                    })
                }
                op => {
                    let op_name: &'static str = op.suffix();
                    let field_name = key.trim_end_matches(op_name);
                    let field = model.fields().find_from_all(&field_name).unwrap();

                    match field {
                        Field::Scalar(s) => {
                            let value: PrismaValue = value.try_into()?;
                            Ok(match op {
                                FilterOp::In => s.is_in(PrismaListValue::try_from(value)?),
                                FilterOp::NotIn => s.not_in(PrismaListValue::try_from(value)?),
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
                            let value: Option<BTreeMap<String, ParsedInputValue>> = value.try_into()?;

                            Ok(match (op, value) {
                                (FilterOp::Some, Some(value)) => {
                                    r.at_least_one_related(extract_filter(value, &r.related_model())?)
                                }
                                (FilterOp::None, Some(value)) => {
                                    r.no_related(extract_filter(value, &r.related_model())?)
                                }
                                (FilterOp::Every, Some(value)) => {
                                    r.every_related(extract_filter(value, &r.related_model())?)
                                }
                                (FilterOp::Field, Some(value)) => {
                                    r.to_one_related(extract_filter(value, &r.related_model())?)
                                }
                                (FilterOp::Field, None) => r.one_relation_is_null(),
                                _ => unreachable!(),
                            })
                        }
                    }
                }
            }
        })
        .collect::<QueryBuilderResult<Vec<Filter>>>()?;

    Ok(Filter::and(filters))
}
