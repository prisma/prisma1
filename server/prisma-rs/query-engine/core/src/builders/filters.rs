use crate::CoreResult;
use connector::filter::Filter;
use graphql_parser::query::Value;
use prisma_models::{ModelRef, Field, PrismaValue};
use std::collections::BTreeMap;
use connector::compare::ScalarCompare;

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

impl From<FilterOp> for &'static str {
    fn from(fo: FilterOp) -> Self {
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
            FilterOp::Field => "<field>",
        }
    }
}

fn to_prisma_value(v: Value) -> PrismaValue {
    match v {
        Value::Boolean(b) => PrismaValue::Boolean(b),
        Value::Enum(e) => PrismaValue::Enum(e),
        Value::Float(f) => PrismaValue::Float(f),
        Value::Int(i) => PrismaValue::Int(i.as_i64().unwrap() as i32),
        Value::Null => PrismaValue::Null,
        Value::String(s) => PrismaValue::String(s),
        _ => unimplemented!(),
    }
}

fn extract_filter(map: &BTreeMap<String, Value>, model: ModelRef) -> CoreResult<Filter> {
    let ops = vec![
        FilterOp::In,
        FilterOp::NotIn,
        FilterOp::Not,
        FilterOp::Lt,
        FilterOp::Lte,
        FilterOp::Gt,
        FilterOp::Gte,
        FilterOp::Contains,
        FilterOp::NotContains,
        FilterOp::StartsWith,
        FilterOp::NotStartsWith,
        FilterOp::EndsWith,
        FilterOp::NotEndsWith,
        FilterOp::Some,
        FilterOp::None,
        FilterOp::Every,
        FilterOp::NestedAnd,
        FilterOp::NestedOr,
        FilterOp::NestedNot,
        FilterOp::Field,
    ];

    map.iter().fold(Ok(None), |_, (k, v)| {
        ops.iter().find(|op| k.as_str() == op.into()).map(|op| {
            let field_name = k.trim_end_matches(op.into());
            let field = model.fields().find_from_all(&field_name).unwrap(); // fixme: unwrap

            let filter = match field {
                Field::Scalar(s) => {
                    let value = to_prisma_value(v);
                    match op {
                        FilterOp::In => unimplemented!(),
                        FilterOp::NotIn => unimplemented!(),
                        FilterOp::Not => s.not_equals(v),
                        FilterOp::Lt => ,
                        FilterOp::Lte => ,
                        FilterOp::Gt => ,
                        FilterOp::Gte => ,
                        FilterOp::Contains => s.contains(v),
                        FilterOp::NotContains => ,
                        FilterOp::StartsWith => ,
                        FilterOp::NotStartsWith => ,
                        FilterOp::EndsWith => ,
                        FilterOp::NotEndsWith => ,
                    }
                },
                Field::Relation(r)=> {
                    match op {
                        FilterOp::Some => ,
                        FilterOp::None => ,
                        FilterOp::Every => ,
                    }
                },
            };

            unimplemented!()
        });

        // let (field_name, op) = match k.as_str() {
        //     // Filters
        //     x if x.ends_with("_in") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_not_in") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_not") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_lt") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_lte") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_gt") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_gte") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_contains") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_not_contains") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_starts_with") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_not_starts_with") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_ends_with") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_not_ends_with") => (x.trim_end_matches("_in"), FilterOp::In),

        //     // Relations
        //     x if x.ends_with("_some") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_none") => (x.trim_end_matches("_in"), FilterOp::In),
        //     x if x.ends_with("_every") => (x.trim_end_matches("_in"), FilterOp::In),

        //     // Nesting
        //     "AND" => {}
        //     "OR" => {}
        //     "NOT" => {}

        //     // Fields
        //     x => {}
        // }

        unimplemented!()
    });

    unimplemented!()
}
