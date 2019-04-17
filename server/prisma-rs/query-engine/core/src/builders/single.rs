use super::BuilderExt;
use crate::{query_ast::RecordQuery, CoreResult};

use graphql_parser::query::Field;
use prisma_models::ModelRef;
use std::sync::Arc;

#[derive(Debug, Default)]
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
        Default::default()
    }

    fn build(self) -> CoreResult<Self::Output> {
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
        let fields = Self::collect_selection_order(&field);

        Ok(RecordQuery {
            name,
            selector,
            selected_fields,
            nested,
            fields,
        })
    }
}
