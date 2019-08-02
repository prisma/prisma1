use super::*;
use crate::query_builders::{Builder, ParsedField, ParsedInputMap, QueryBuilderResult};
use connector::write_ast::*;
use prisma_models::ModelRef;
use std::convert::TryInto;

pub struct CreateBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl CreateBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder<WriteQuery> for CreateBuilder {
    fn build(mut self) -> QueryBuilderResult<WriteQuery> {
        let data_argument = self.field.arguments.lookup("data").unwrap();
        let data_map: ParsedInputMap = data_argument.value.try_into()?;

        Self::build_from(self.model, data_map).map(|cr| cr.into())
    }
}

impl CreateBuilder {
    pub fn build_from(model: ModelRef, data: ParsedInputMap) -> QueryBuilderResult<CreateRecord> {
        let create_args = WriteArguments::from(&model, data, true)?;
        Ok(CreateRecord {
            model: model,
            non_list_args: create_args.non_list,
            list_args: create_args.list,
            nested_writes: create_args.nested,
        })
    }
}
