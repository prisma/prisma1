use super::*;
use crate::query_builders::{Builder, ParsedField, ParsedInputMap, QueryBuilderResult};
use connector::write_ast::*;
use prisma_models::ModelRef;
use std::convert::TryInto;

pub struct DeleteBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl DeleteBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder<WriteQuery> for DeleteBuilder {
    fn build(mut self) -> QueryBuilderResult<WriteQuery> {
        let where_arg = self.field.arguments.lookup("where").unwrap();
        let record_finder = utils::extract_record_finder(where_arg.value, &self.model)?;

        Ok(DeleteRecord { where_: record_finder }.into())
    }
}
