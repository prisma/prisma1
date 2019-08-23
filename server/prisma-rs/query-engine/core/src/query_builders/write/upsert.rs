use super::*;
use crate::query_builders::{Builder, ParsedField, QueryBuilderResult};
use connector::write_ast::*;
use prisma_models::ModelRef;
use std::{convert::TryInto, sync::Arc};

pub struct UpsertBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl UpsertBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder<WriteQuery> for UpsertBuilder {
    fn build(mut self) -> QueryBuilderResult<WriteQuery> {
        let where_arg = self.field.arguments.lookup("where").unwrap();
        let record_finder = utils::extract_record_finder(where_arg.value, &self.model)?;

        let create_argument = self.field.arguments.lookup("create").unwrap();
        let update_argument = self.field.arguments.lookup("update").unwrap();

        let create = CreateBuilder::build_from(Arc::clone(&self.model), create_argument.value.try_into()?)?;
        let update = UpdateBuilder::build_from(
            Arc::clone(&self.model),
            update_argument.value.try_into()?,
            record_finder.clone(),
        )?;

        let upsert = RootWriteQuery::UpsertRecord(Box::new(UpsertRecord {
            where_: record_finder,
            create,
            update,
        }));

        Ok(WriteQuery::Root(self.field.name, self.field.alias, upsert))
    }
}
