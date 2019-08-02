use super::*;
use crate::query_builders::{Builder, ParsedField, ParsedInputMap, QueryBuilderResult};
use connector::{filter::RecordFinder, write_ast::*};
use prisma_models::ModelRef;
use std::convert::TryInto;

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
        // "where"
        let where_arg = self.field.arguments.lookup("where").unwrap();
        let record_finder = utils::extract_record_finder(where_arg.value, &self.model)?;

        // "data"
        let data_argument = self.field.arguments.lookup("data").unwrap();
        let data_map: ParsedInputMap = data_argument.value.try_into()?;

        Self::build_from(self.model, data_map, record_finder).map(|ur| ur.into())
    }
}

impl UpdateBuilder {
    pub fn build_from(model: ModelRef, data: ParsedInputMap, record_finder: RecordFinder) -> QueryBuilderResult<UpdateRecord> {
        let update_args = WriteArguments::from(&model, data, false)?;
        Ok(UpdateRecord {
            where_: record_finder,
            non_list_args: update_args.non_list,
            list_args: update_args.list,
            nested_writes: update_args.nested,
        })
    }
}
