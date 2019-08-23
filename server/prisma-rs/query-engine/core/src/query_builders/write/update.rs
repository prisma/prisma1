use super::*;
use crate::query_builders::{Builder, ParsedField, ParsedInputMap, QueryBuilderResult};
use connector::{filter::RecordFinder, write_ast::*};
use prisma_models::ModelRef;
use std::convert::TryInto;
use std::sync::Arc;

pub struct UpdateBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl UpdateBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder<WriteQuery> for UpdateBuilder {
    fn build(mut self) -> QueryBuilderResult<WriteQuery> {
        let model = self.model;
        let name = self.field.name;
        let alias = self.field.alias;

        // "where"
        let where_arg = self.field.arguments.lookup("where").unwrap();
        let record_finder = utils::extract_record_finder(where_arg.value, &model)?;

        // "data"
        let data_argument = self.field.arguments.lookup("data").unwrap();
        let data_map: ParsedInputMap = data_argument.value.try_into()?;

        Self::build_from(model, data_map, record_finder)
            .map(|ur| WriteQuery::Root(name, alias, RootWriteQuery::UpdateRecord(Box::new(ur))))
    }
}

impl UpdateBuilder {
    pub fn build_from(
        model: ModelRef,
        data: ParsedInputMap,
        record_finder: RecordFinder,
    ) -> QueryBuilderResult<UpdateRecord> {
        let update_args = WriteArguments::from(&model, data, false)?;
        let list_causes_update = !update_args.list.is_empty();
        let mut non_list_args = update_args.non_list;
        non_list_args.update_datetimes(Arc::clone(&model), list_causes_update);
        Ok(UpdateRecord {
            where_: record_finder,
            non_list_args,
            list_args: update_args.list,
            nested_writes: update_args.nested,
        })
    }
}
