use super::*;
use crate::query_builders::{utils, ParsedField, QueryBuilderResult, Builder};
use connector::read_ast::{ManyRecordsQuery, ReadQuery};
use prisma_models::ModelRef;

pub struct ReadManyRecordsBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl ReadManyRecordsBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder<ReadQuery> for ReadManyRecordsBuilder {
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        let args = utils::extract_query_args(self.field.arguments, &self.model)?;
        let name = self.field.alias.unwrap_or(self.field.name);
        let sub_selections = self.field.sub_selections.unwrap().fields;
        let selection_order: Vec<String> = collect_selection_order(&sub_selections);
        let selected_fields = collect_selected_fields(&sub_selections, &self.model, None);
        let nested = collect_nested_queries(sub_selections, &self.model)?;
        let model = self.model;

        Ok(ReadQuery::ManyRecordsQuery(ManyRecordsQuery {
            name,
            model,
            args,
            selected_fields,
            nested,
            selection_order,
        }))
    }
}
