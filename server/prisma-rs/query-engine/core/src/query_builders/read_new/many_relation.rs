use super::Builder;
use crate::query_builders::{utils, ParsedField, QueryBuilderResult};
use connector::read_ast::ReadQuery;
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
        let query_args = utils::extract_query_args(self.field.arguments);
        dbg!(query_args);

        unimplemented!()
    }
}
