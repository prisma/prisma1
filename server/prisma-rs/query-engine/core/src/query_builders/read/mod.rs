//! Read query builders module

mod filters;
mod many;
mod many_rel;
mod one;
mod one_rel;

pub(crate) mod utils;

pub use many::*;
pub use many_rel::*;
pub use one::*;
pub use one_rel::*;

use crate::{schema::OperationTag, CoreError, CoreResult, ModelOperation, QuerySchemaRef};
use connector::read_ast::ReadQuery;
use graphql_parser::query::Field;
use prisma_models::{ModelRef, RelationFieldRef};
use std::sync::Arc;

/// A common query-builder type
#[derive(Debug)]
pub enum ReadQueryBuilder<'field> {
    One(OneBuilder<'field>),
    Many(ManyBuilder<'field>),
    OneRelation(OneRelationBuilder<'field>),
    ManyRelation(ManyRelationBuilder<'field>),
}

impl<'a> ReadQueryBuilder<'a> {
    pub fn new(query_schema: QuerySchemaRef, root_field: &'a Field) -> CoreResult<Self> {
        let query_field = match query_schema.find_query_field(root_field.name.as_ref()) {
            Some(field) => Ok(field),
            None => Err(CoreError::LegacyQueryValidationError(format!(
                "Field not found on type Query: {}",
                &root_field.name
            ))),
        }?;

        let model_operation = query_field
            .operation
            .clone()
            .expect("Expected top level field to have an associated model operation.");

        ReadQueryBuilder::infer_root(model_operation, root_field)
    }

    /// Infer the type of builder that should be created for a root field.
    fn infer_root(model_operation: ModelOperation, field: &'a Field) -> CoreResult<ReadQueryBuilder<'a>> {
        let model = model_operation.model;
        let operation = model_operation.operation;

        match operation {
            OperationTag::FindOne => Ok(ReadQueryBuilder::One(
                OneBuilder::new().setup(Arc::clone(&model), field),
            )),
            OperationTag::FindMany => Ok(ReadQueryBuilder::Many(
                ManyBuilder::new().setup(Arc::clone(&model), field),
            )),
            _ => Err(CoreError::LegacyQueryValidationError(format!(
                "Invalid root operation on Query: {:?}",
                operation
            ))),
        }
    }

    /// Temporary workaround until we have a full query schema integration.
    fn infer_nested(
        model: &ModelRef,
        field: &'a Field,
        parent: Option<RelationFieldRef>,
    ) -> Option<ReadQueryBuilder<'a>> {
        if let Some(ref parent) = parent {
            if parent.is_list {
                Some(ReadQueryBuilder::ManyRelation(ManyRelationBuilder::new().setup(
                    Arc::clone(&model),
                    field,
                    Arc::clone(&parent),
                )))
            } else {
                Some(ReadQueryBuilder::OneRelation(OneRelationBuilder::new().setup(
                    Arc::clone(&model),
                    field,
                    Arc::clone(&parent),
                )))
            }
        } else {
            None
        }
    }

    pub fn build(self) -> CoreResult<ReadQuery> {
        match self {
            ReadQueryBuilder::One(b) => Ok(ReadQuery::RecordQuery(b.build()?)),
            ReadQueryBuilder::Many(b) => Ok(ReadQuery::ManyRecordsQuery(b.build()?)),
            ReadQueryBuilder::OneRelation(b) => Ok(ReadQuery::RelatedRecordQuery(b.build()?)),
            ReadQueryBuilder::ManyRelation(b) => Ok(ReadQuery::ManyRelatedRecordsQuery(b.build()?)),
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
