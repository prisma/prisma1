//! Query execution builders module

mod filters;
mod inflector;
mod many;
mod many_rel;
mod mutations;
mod one_rel;
mod root;
mod single;

pub(crate) mod utils;

pub use many::*;
pub use many_rel::*;
pub use mutations::*;
pub use one_rel::*;
pub use root::*;
pub use single::*;

pub use self::inflector::Inflector;

use crate::{CoreError, CoreResult, ReadQuery};
use graphql_parser::query::Field;
use prisma_models::{InternalDataModelRef, ModelRef, RelationFieldRef};
use rust_inflector::Inflector as RustInflector;

use std::sync::Arc;

/// A common query-builder type
#[derive(Debug)]
pub enum Builder<'field> {
    Single(SingleBuilder<'field>),
    Many(ManyBuilder<'field>),
    OneRelation(OneRelationBuilder<'field>),
    ManyRelation(ManyRelationBuilder<'field>),
}

impl<'a> Builder<'a> {
    fn new(internal_data_model: InternalDataModelRef, root_field: &'a Field) -> CoreResult<Self> {
        // Find model for field - this is a temporary workaround before we have a data model definition (/ internal_data_model builder).
        let builder: Option<Builder> = internal_data_model
            .models()
            .iter()
            .filter_map(|model| Builder::infer(model, root_field, None))
            .nth(0);

        match builder {
            Some(b) => Ok(b),
            None => Err(CoreError::QueryValidationError(format!(
                "Model not found for field {}",
                root_field.alias.as_ref().unwrap_or(&root_field.name)
            ))),
        }
    }

    /// Infer the type of builder that should be created
    fn infer(model: &ModelRef, field: &'a Field, parent: Option<RelationFieldRef>) -> Option<Builder<'a>> {
        if let Some(ref parent) = parent {
            if parent.is_list {
                Some(Builder::ManyRelation(ManyRelationBuilder::new().setup(
                    Arc::clone(&model),
                    field,
                    Arc::clone(&parent),
                )))
            } else {
                Some(Builder::OneRelation(OneRelationBuilder::new().setup(
                    Arc::clone(&model),
                    field,
                    Arc::clone(&parent),
                )))
            }
        } else {
            let normalized = match model.name.as_str() {
                "AUser" => "aUser".to_owned(), // FIXME *quietly sobbing*
                name => name.to_camel_case(),
            };

            if field.name == normalized {
                Some(Builder::Single(SingleBuilder::new().setup(Arc::clone(model), field)))
            } else if Inflector::singularize(&field.name) == normalized {
                Some(Builder::Many(ManyBuilder::new().setup(Arc::clone(model), field)))
            } else {
                None
            }
        }
    }

    fn build(self) -> CoreResult<ReadQuery> {
        match self {
            Builder::Single(b) => Ok(ReadQuery::RecordQuery(b.build()?)),
            Builder::Many(b) => Ok(ReadQuery::ManyRecordsQuery(b.build()?)),
            Builder::OneRelation(b) => Ok(ReadQuery::RelatedRecordQuery(b.build()?)),
            Builder::ManyRelation(b) => Ok(ReadQuery::ManyRelatedRecordsQuery(b.build()?)),
        }
    }
}

/// A trait that describes a query builder
pub trait BuilderExt {
    type Output;

    /// A common cosntructor for all query builders
    fn new() -> Self;

    /// Last step that invokes query building
    fn build(self) -> CoreResult<Self::Output>;
}
