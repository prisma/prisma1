//! Query execution builders module
#![allow(dead_code)]

mod single;
mod multi;
mod one_rel;
mod many_rel;

use graphql_parser::query::Field;
use prisma_models::ModelRef;
use inflector::Inflector;

pub enum Builder {
    Single(single::Builder),
    Multi(multi::Builder),
    Rel(one_rel::Builder),
    ManyRel(many_rel::Builder)
}

impl Builder {

    /// Infer the type of builder that should be created
    fn infer(model: &ModelRef, field: &Field) -> Option<Self> {
        if model.name.to_camel_case().to_singular() == field.name {
            Some(Builder::Single(single::Builder))
        } else if model.name.to_camel_case().to_plural() == field.name {
            Some(Builder::Multi(multi::Builder))
        } else {
            None
        }
    }
}

/// A trait that describes a query builder
pub trait BuilderExt {
    type Output;

    /// Last step that invokes query building
    fn build(self) -> Self::Output;
}
