use super::*;
use crate::query_builders::{Builder, ParsedField, ParsedInputMap, QueryBuilderResult};
use connector::filter::Filter;
use connector::write_ast::*;
use prisma_models::ModelRef;
use std::convert::TryInto;
use std::sync::Arc;

pub struct UpdateManyBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl UpdateManyBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder<WriteQuery> for UpdateManyBuilder {
    fn build(mut self) -> QueryBuilderResult<WriteQuery> {
        let filter = match self.field.arguments.lookup("where") {
            Some(where_arg) => extract_filter(where_arg.value.try_into()?, &self.model)?,
            None => Filter::empty(),
        };

        let data_argument = self.field.arguments.lookup("data").unwrap();
        let data_map: ParsedInputMap = data_argument.value.try_into()?;
        let update_args = WriteArguments::from(&self.model, data_map, true)?;
        let list_causes_update = update_args.list.len() > 0;
        let mut non_list_args = update_args.non_list;
        non_list_args.update_datetimes(Arc::clone(&self.model), list_causes_update);

        let update_many = RootWriteQuery::UpdateManyRecords(UpdateManyRecords {
            model: self.model,
            filter: filter,
            non_list_args: non_list_args,
            list_args: update_args.list,
        });

        Ok(WriteQuery::Root(self.field.name, self.field.alias, update_many))
    }
}
