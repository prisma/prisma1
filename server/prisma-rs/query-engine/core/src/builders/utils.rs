//! A set of utilities to build (read & write) queries

use graphql_parser::query::{Field, Value};
use prisma_models::{ModelRef, PrismaValue, GraphqlId};
use connector::filter::NodeSelector;
use crate::CoreResult;

use std::sync::Arc;
use std::collections::BTreeMap;

/// Get node selector from field and model
pub(crate) fn extract_node_selector(field: &Field, model: ModelRef) -> CoreResult<NodeSelector> {

    // FIXME: this expects at least one query arg...
    let (_, value) = field.arguments.first().expect("no arguments found");
    match value {
        Value::Object(obj) => {
            let (field_name, value) = obj.iter().next().expect("object was empty");
            let field = model.fields().find_from_scalar(field_name).unwrap();
            let value = PrismaValue::from_value(value);

            Ok(NodeSelector {
                field: Arc::clone(&field),
                value: value,
            })
        }
        _ => unimplemented!(),
    }
}

/// A function that derives a field given a field
///
/// This function is used when creating ReadQueries after a WriteQuery has succeeded
pub(crate) fn derive_field(field: &Field, model: ModelRef, id: GraphqlId) -> Field {
    let mut new = field.clone();

    // Remove alias and override Name
    new.name = model.name.to_lowercase();
    new.alias = None;

    // Create a selection set for this ID
    let id_name = model.fields().id().name.clone();
    let mut map = BTreeMap::new();
    map.insert(id_name, id.to_value());

    // Then override the existing arguments
    new.arguments = vec![
        ("where".into(), Value::Object(map))
    ];

    new
}