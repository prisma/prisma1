use super::{utils, BuilderExt};
use crate::{read_query_ast::ManyRelatedRecordsQuery, CoreResult};

use graphql_parser::query::Field;
use prisma_models::{ModelRef, RelationFieldRef};
use std::sync::Arc;

#[derive(Default, Debug)]
pub struct ManyRelationBuilder<'f> {
    model: Option<ModelRef>,
    field: Option<&'f Field>,
    parent: Option<RelationFieldRef>,
}

impl<'f> ManyRelationBuilder<'f> {
    pub fn setup(self, model: ModelRef, field: &'f Field, parent: RelationFieldRef) -> Self {
        Self {
            model: Some(model),
            field: Some(field),
            parent: Some(parent),
        }
    }
}

impl<'f> BuilderExt for ManyRelationBuilder<'f> {
    type Output = ManyRelatedRecordsQuery;

    fn new() -> Self {
        Default::default()
    }

    fn build(self) -> CoreResult<Self::Output> {
        let (model, field, parent) = match (&self.model, &self.field, &self.parent) {
            (Some(m), Some(f), Some(p)) => Some((m, f, p)),
            _ => None,
        }
        .expect("`ManyRelatedRecordsQuery` builder not properly initialized!");

        let nested_builders = utils::collect_nested_queries(Arc::clone(&model), field, model.internal_data_model())?;
        let nested = utils::build_nested_queries(nested_builders)?;

        let parent_field = Arc::clone(parent);
        let selected_fields = utils::collect_selected_fields(Arc::clone(&model), field, Arc::clone(&parent))?;
        let args = utils::extract_query_args(field, Arc::clone(&model))?;
        let name = field.alias.as_ref().unwrap_or(&field.name).clone();
        let fields = utils::collect_selection_order(&field);

        Ok(ManyRelatedRecordsQuery {
            name,
            parent_field,
            args,
            selected_fields,
            nested,
            fields,
        })
    }
}
