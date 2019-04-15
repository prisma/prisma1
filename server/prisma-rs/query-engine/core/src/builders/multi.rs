use super::{Builder as SuperBuilder, BuilderExt};
use crate::{query_ast::MultiRecordQuery, CoreError, CoreResult};

use connector::filter::NodeSelector;
use graphql_parser::query::Field;
use prisma_models::{ModelRef, RelationFieldRef, SelectedFields};
use std::sync::Arc;

pub struct MultiBuilder<'f> {
    model: Option<ModelRef>,
    field: Option<&'f Field>,
}

impl<'f> MultiBuilder<'f> {
    pub fn setup(self, model: ModelRef, field: &'f Field) -> Self {
        Self {
            model: Some(model),
            field: Some(field),
        }
    }
}

impl<'f> BuilderExt for MultiBuilder<'f> {
    type Output = MultiRecordQuery;

    fn new() -> Self {
        Self {
            model: None,
            field: None,
        }
    }

    fn build(self) -> CoreResult<Self::Output> {
        let (model, field) = match (&self.model, &self.field) {
            (Some(m), Some(f)) => Some((m, f)),
            _ => None,
        }
        .expect("`MultiQuery` builder not properly initialised!");

        let nested_builders = Self::collect_nested_queries(Arc::clone(&model), field, model.schema())?;
        let nested = Self::build_nested_queries(nested_builders)?;

        let selected_fields = Self::collect_selected_fields(Arc::clone(&model), field, None)?;
        let args = Self::extract_query_args(field, Arc::clone(&model))?;
        let name = field.alias.as_ref().unwrap_or(&field.name).clone();
        let model = Arc::clone(model);

        Ok(MultiRecordQuery {
            name,
            model,
            args,
            selected_fields,
            nested,
        })
    }
}
