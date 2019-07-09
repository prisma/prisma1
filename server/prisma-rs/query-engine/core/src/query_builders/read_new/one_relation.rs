use super::Builder;
use crate::query_builders::{utils, ParsedField, QueryBuilderResult};
use connector::read_ast::ReadQuery;
use prisma_models::{ModelRef, RelationFieldRef};

pub struct ReadOneRelationRecordBuilder {
    /// The model that is queried.
    model: ModelRef,

    /// The relation field on the parent model.
    parent: RelationFieldRef,

    /// The parent field as parsed field in the query document.
    field: ParsedField,
}

impl ReadOneRelationRecordBuilder {
    pub fn new(model: ModelRef, parent: RelationFieldRef, field: ParsedField) -> Self {
        Self { model, parent, field }
    }
}

impl Builder for ReadOneRelationRecordBuilder {
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        // Unwrap: Relation field requires sub selection.
        let query_args = utils::extract_query_args(self.field.arguments);

        // Ok(RelatedRecordQuery {
        //     name,
        //     parent_field,
        //     args,
        //     selected_fields,
        //     nested,
        //     fields,
        // })
        unimplemented!()
    }
}
