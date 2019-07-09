use super::Builder;
use crate::query_builders::{ParsedField, QueryBuilderResult};
use connector::read_ast::ReadQuery;
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

impl Builder for ReadManyRecordsBuilder {
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        unimplemented!()
    }
}
