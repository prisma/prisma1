use super::{Builder as SuperBuilder, BuilderExt};
use crate::{query_ast::RecordQuery, CoreError, CoreResult};

use connector::filter::NodeSelector;
use graphql_parser::query::Field;
use prisma_models::{ModelRef, SelectedFields};
use std::sync::Arc;

pub struct Builder<'f> {
    model: Option<ModelRef>,
    field: Option<&'f Field>,
    selector: Option<NodeSelector>,
}

impl<'f> BuilderExt for Builder<'f> {
    type Output = RecordQuery;

    fn new() -> Self {
        Self {
            model: None,
            field: None,
            selector: None,
        }
    }

    fn build(mut self) -> CoreResult<Self::Output> {
        let (model, field, selector) = match (&self.model, &self.field, &self.selector) {
            (Some(m), Some(f), Some(s)) => Some((m, f, s)),
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

impl<'f> Builder<'f> {
    pub fn setup(self, model: ModelRef, field: &'f Field, selector: NodeSelector) -> Self {
        Self {
            model: Some(model),
            field: Some(field),
            selector: Some(selector),
        }
    }
}
