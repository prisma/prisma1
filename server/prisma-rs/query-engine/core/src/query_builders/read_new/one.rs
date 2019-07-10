use super::*;
use crate::query_builders::{utils, ParsedField, QueryBuilderResult};
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

impl Builder for ReadOneRecordBuilder {
    /// Builds a read query tree from a parsed top-level field of a query
    /// Unwraps are safe because of query validation that ensures conformity to the query schema.
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        let record_finder = utils::extract_record_finder(self.field.arguments, &self.model)?;
        let name = self.field.alias.unwrap_or(self.field.name);
        let sub_selections = self.field.sub_selections.unwrap().fields;
        let selection_order: Vec<String> = collect_selection_order(&sub_selections);
        let selected_fields = collect_selected_fields(&sub_selections, &self.model, None);
        let nested = collect_nested_queries(sub_selections, &self.model)?;

        Ok(ReadQuery::RecordQuery(RecordQuery {
            name,
            record_finder,
            selected_fields,
            nested,
            selection_order,
        }))
    }
}
