use super::{Builder as SuperBuilder, BuilderExt};
use crate::{query_ast::RecordQuery, CoreError, CoreResult};

use connector::filter::NodeSelector;
use graphql_parser::query::Field;
use prisma_models::{ModelRef, SelectedFields};
use std::sync::Arc;

#[derive(Debug)]
pub struct SingleBuilder<'f> {
    model: Option<ModelRef>,
    field: Option<&'f Field>,
}

impl<'f> SingleBuilder<'f> {
    pub fn setup(self, model: ModelRef, field: &'f Field) -> Self {
        Self {
            model: Some(model),
            field: Some(field),
        }
    }
}

impl<'f> BuilderExt for SingleBuilder<'f> {
    type Output = RecordQuery;

    fn new() -> Self {
        Self {
            model: None,
            field: None,
        }
    }

    fn build(mut self) -> CoreResult<Self::Output> {
        let (model, field) = match (&self.model, &self.field) {
            (Some(m), Some(f)) => Some((m, f)),
            _ => None,
        }
        .expect("`RecordQuery` builder not properly initialised!");

        let nested_builders = Self::collect_nested_queries(Arc::clone(&model), field, model.schema())?;
        let nested = Self::build_nested_queries(nested_builders)?;

        let selected_fields = Self::collect_selected_fields(Arc::clone(&model), field, None)?;
        let selector = Self::extract_node_selector(&field, Arc::clone(&model))?;
        let name = field.alias.as_ref().unwrap_or(&field.name).clone();

        Ok(RecordQuery {
            name,
            selector,
            selected_fields,
            nested,
        })
    }
}
