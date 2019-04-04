use super::{Builder as BuilderEnum, BuilderExt};
use crate::query_ast::RecordQuery;

use connector::NodeSelector;
use graphql_parser::query::Field;
use prisma_models::{ModelRef, SelectedFields};
use std::sync::Arc;

pub struct Builder<'a> {
    model: ModelRef,
    field: &'a Field,
    selector: NodeSelector,
    selected_fields: opt!(SelectedFields),
    nested: Vec<BuilderEnum<'a>>,
}

impl<'a> BuilderExt for Builder<'a> {
    type Output = RecordQuery;

    fn new() -> Self {
        unimplemented!()
    }

    fn build(self) -> Self::Output {
        unimplemented!()
    }
}

impl<'a> Builder<'a> {

    fn map_selected_scalar_fields(&mut self) {
        self.selected_fields = Some(
            Self::collect_selected_fields(Arc::clone(&self.model), self.field)
                .map(|sf| SelectedFields::new(sf, None))
                .unwrap(),
        );
    }

    // Todo: Maybe we can merge this with the map selected fields somehow, as the code looks fairly similar
    fn collect_nested_queries(&mut self) {
        // self.nested = Self::collect_nested_queries(self.model, self.field);
    }
}
