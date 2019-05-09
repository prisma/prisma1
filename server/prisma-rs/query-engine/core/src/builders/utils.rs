//! A set of utilities to build (read & write) queries

use graphql_parser::query::{Field, Value};
use prisma_models::{ModelRef, PrismaValue};
use connector::filter::NodeSelector;
use crate::CoreResult;

use std::sync::Arc;

/// Get node selector from field and model
pub(crate) fn extract_node_selector(field: &Field, model: ModelRef) -> CoreResult<NodeSelector> {

    // FIXME: this expects at least one query arg...
    let (_, value) = field.arguments.first().expect("no arguments found");
    match value {
        Value::Object(obj) => {
            let (field_name, value) = obj.iter().next().expect("object was empty");
            let field = model.fields().find_from_scalar(field_name).unwrap();
            let value = value_to_prisma_value(value);

            Ok(NodeSelector {
                field: Arc::clone(&field),
                value: value,
            })
        }
        _ => unimplemented!(),
    }
}

    /// Turning a GraphQL value to a PrismaValue
pub(crate) fn value_to_prisma_value(val: &Value) -> PrismaValue {
    match val {
        Value::String(ref s) => match serde_json::from_str(s) {
            Ok(val) => PrismaValue::Json(val),
            _ => PrismaValue::String(s.clone()),
        },
        Value::Int(i) => PrismaValue::Int(i.as_i64().unwrap()),
        _ => unimplemented!(),
    }
}
