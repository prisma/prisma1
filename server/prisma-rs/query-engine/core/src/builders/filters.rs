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

fn extract_filter() -> () {
    map.iter().fold(Ok(aggregator), |prev, (k, v)| {
        match k.as_str() {
            // Filters
            x if x.ends_with("_in") => (x.split(), Filters::IN),
            x if x.ends_with("_not_in") => {}
            x if x.ends_with("_not") => {}
            x if x.ends_with("_lt") => {}
            x if x.ends_with("_lte") => {}
            x if x.ends_with("_gt") => {}
            x if x.ends_with("_gte") => {}
            x if x.ends_with("_contains") => {}
            x if x.ends_with("_not_contains") => {}
            x if x.ends_with("_starts_with") => {}
            x if x.ends_with("_not_starts_with") => {}
            x if x.ends_with("_ends_with") => {}
            x if x.ends_with("_not_ends_with") => {}

            // Relations
            x if x.ends_with("_some") => {}
            x if x.ends_with("_none") => {}
            x if x.ends_with("_every") => {}

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
