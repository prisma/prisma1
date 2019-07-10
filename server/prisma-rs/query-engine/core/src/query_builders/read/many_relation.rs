use super::*;
use crate::query_builders::{utils, ParsedField, QueryBuilderResult};
use connector::read_ast::{ManyRelatedRecordsQuery, ReadQuery};
use prisma_models::{ModelRef, RelationFieldRef};

pub struct ReadManyRelationRecordsBuilder {
    /// The model that is queried.
    model: ModelRef,

    /// The relation field on the parent model.
    parent: RelationFieldRef,

    /// The parent field as parsed field in the query document.
    field: ParsedField,
}

impl ReadManyRelationRecordsBuilder {
    pub fn new(model: ModelRef, parent: RelationFieldRef, field: ParsedField) -> Self {
        Self { model, parent, field }
    }
}

impl Builder for ReadManyRelationRecordsBuilder {
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        let args = utils::extract_query_args(self.field.arguments, &self.model)?;
        let name = self.field.alias.unwrap_or(self.field.name);
        let sub_selections = self.field.sub_selections.unwrap().fields;
        let selection_order: Vec<String> = collect_selection_order(&sub_selections);
        let selected_fields = collect_selected_fields(&sub_selections, &self.model, Some(Arc::clone(&self.parent)));
        let nested = collect_nested_queries(sub_selections, &self.model)?;
        let parent_field = self.parent;

        Ok(ReadQuery::ManyRelatedRecordsQuery(ManyRelatedRecordsQuery {
            name,
            parent_field,
            args,
            selected_fields,
            nested,
            selection_order,
        }))
    }
}
