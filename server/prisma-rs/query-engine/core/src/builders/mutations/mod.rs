//! Mutation builder module

mod ast;
mod look_ahead;
mod many;
mod parser;
mod results;
mod root;
mod simple;

pub use ast::*;
pub use look_ahead::*;
pub use parser::*;
pub use results::*;

// Mutation builder modules
pub use many::*;
pub use root::*;
pub use simple::*;

use prisma_models::{ModelRef, PrismaValue};
use std::collections::BTreeMap;

/// Extends given BTreeMap with defaults for fields from hte given model if the fields are not
/// already present in the map.
pub(crate) fn extend_defaults(model: &ModelRef, args: &mut BTreeMap<String, PrismaValue>) {
    // Defaults for scalar non-list fields
    let defaults: Vec<(String, PrismaValue)> = model
        .fields()
        .scalar()
        .into_iter()
        .filter_map(|sf| sf.default_value.clone().map(|dv| (sf.name.clone(), dv)))
        .collect();

    // Fold defaults into args, if field is not already set.
    defaults.into_iter().for_each(|d| {
        if !args.contains_key(&d.0) {
            args.insert(d.0, d.1);
        }
    });
}
