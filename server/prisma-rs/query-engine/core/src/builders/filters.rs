use connector::{
    filter::{Filter},
    QueryArguments,
};
use std::{collections::BTreeMap, sync::Arc};
use graphql_parser::query::{Value};

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

fn extract_filter(aggregator: QueryArguments,
        map: &BTreeMap<String, Value>,
        model: ModelRef,) -> CoreResult<Filter> {
    map.iter().fold(Ok(aggregator), |prev, (k, v)| {
        let match k.as_str() {
            // Filters
            x if x.ends_with("_in") => (x.trim_end_matches("_in"), FilterOp::In),
            x if x.ends_with("_not_in") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_not") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_lt") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_lte") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_gt") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_gte") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_contains") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_not_contains") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_starts_with") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_not_starts_with") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_ends_with") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_not_ends_with") => (x.trim_end_matches("_in"), FilterOp::In)

            // Relations
            x if x.ends_with("_some") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_none") => (x.trim_end_matches("_in"), FilterOp::In)
            x if x.ends_with("_every") => (x.trim_end_matches("_in"), FilterOp::In)

            // Nesting
            "AND" => {}
            "OR" => {}
            "NOT" => {}

            // Fields
            x => {}
        }
        unimplemented!()
    })
}
