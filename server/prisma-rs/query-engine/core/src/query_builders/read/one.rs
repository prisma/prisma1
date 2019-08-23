use super::*;
use crate::query_builders::{utils, Builder, ParsedField, QueryBuilderResult};
use connector::read_ast::{ReadQuery, RecordQuery};
use prisma_models::ModelRef;

pub struct ReadOneRecordBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl ReadOneRecordBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder<ReadQuery> for ReadOneRecordBuilder {
    /// Builds a read query tree from a parsed top-level field of a query
    /// Unwraps are safe because of query validation that ensures conformity to the query schema.
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        let record_finder = match self.field.arguments.into_iter().find(|arg| arg.name == "where") {
            Some(where_arg) => Some(utils::extract_record_finder(where_arg.value, &self.model)?),
            None => None,
        };

        let name = self.field.name;
        let alias = self.field.alias;
        let sub_selections = self.field.sub_selections.unwrap().fields;
        let selection_order: Vec<String> = collect_selection_order(&sub_selections);
        let selected_fields = collect_selected_fields(&sub_selections, &self.model, None);
        let nested = collect_nested_queries(sub_selections, &self.model)?;

        Ok(ReadQuery::RecordQuery(RecordQuery {
            name,
            alias,
            record_finder,
            selected_fields,
            nested,
            selection_order,
        }))
    }
}
