use super::{Builder as SuperBuilder, BuilderExt};
use crate::{query_ast::RecordQuery, CoreError, CoreResult};

use connector::NodeSelector;
use graphql_parser::query::Field;
use prisma_models::{ModelRef, SelectedFields};
use std::sync::Arc;

pub struct Builder<'a> {
    model: Option<ModelRef>,
    field: Option<&'a Field>,
    selector: Option<NodeSelector>,
}

impl<'a> BuilderExt for Builder<'a> {
    type Output = RecordQuery;

    fn new() -> Self {
        Self {
            model: None,
            field: None,
            selector: None,
        }
    }

    fn build(mut self) -> CoreResult<Self::Output> {
        let model = self.model.as_ref().unwrap();
        let field = self.field.as_ref().unwrap();
        let selector = self.selector.as_ref().unwrap();

        let nested_builders = self.get_nested_queries()?;
        let nested = Self::run_nested_queries(nested_builders);

        let selected_fields = self.get_selected_fields()?;
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

impl<'a> Builder<'a> {
    fn setup(self, model: ModelRef, field: &'a Field, selector: NodeSelector) -> Self {
        Self {
            model: Some(model),
            field: Some(field),
            selector: Some(selector),
        }
    }

    fn get_selected_fields(&self) -> CoreResult<SelectedFields> {
        let model = self.model.as_ref().unwrap();
        let field = self.field.as_ref().unwrap();

        Self::collect_selected_fields(Arc::clone(&model), field).map(|sf| SelectedFields::new(sf, None))
    }

    fn get_nested_queries(&self) -> CoreResult<Vec<SuperBuilder>> {
        let model = self.model.as_ref().unwrap();
        let field = self.field.as_ref().unwrap();

        Self::collect_nested_queries(Arc::clone(&model), field, model.schema())
    }
}
