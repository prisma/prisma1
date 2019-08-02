use super::*;
use crate::query_builders::{extract_filter, Builder, ParsedField, QueryBuilderResult};
use connector::write_ast::*;
use prisma_models::ModelRef;
use std::convert::TryInto;

pub struct DeleteManyBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl DeleteManyBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder<WriteQuery> for DeleteManyBuilder {
    fn build(mut self) -> QueryBuilderResult<WriteQuery> {
        let filter = match self.field.arguments.lookup("where") {
            Some(where_arg) => Some(extract_filter(where_arg.value.try_into()?, &self.model)?),
            None => None,
        };

        let delete_many = RootWriteQuery::DeleteManyRecords(DeleteManyRecords {
            model: self.model,
            filter: filter.unwrap(), // TODO: In the schema that's optional, the db interface expects it, though.
        });

        Ok(WriteQuery::Root(self.field.name, self.field.alias, delete_many))
    }
}
