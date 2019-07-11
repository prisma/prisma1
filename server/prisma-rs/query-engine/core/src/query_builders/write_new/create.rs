use super::*;
use crate::query_builders::{utils, ParsedField, ParsedInputValue, QueryBuilderResult};
use connector::write_ast::{WriteQuery, RootWriteQuery, CreateRecord, NestedWriteQueries};
use prisma_models::{Field, ModelRef, PrismaArgs, PrismaListValue, PrismaValue};

pub struct CreateBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl CreateBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl CreateBuilder {
    fn build(self) -> QueryBuilderResult<WriteQuery> {

        // let data_argument = self.field.arguments.into_iter().next().expect("Data argument is missing");
        // let as_map = data_argument.value.to_map()?;

        // let mut non_list_args: PrismaArgs = PrismaArgs::new();
        // let mut list_args: Vec<(String, PrismaListValue)> = Vec::new();

        // // TODO: those pattern matches hint at the fact that i need a function to convert to a list of flat ParsedInputValues
        // // i basically disallow ParsedInputValue::Map in these cases.
        // for (field_name, value) in as_map {
        //     let field = self.model.fields().find_from_all(&field_name).expect("Field not found");
        //     if field.is_list() {
        //         match value {
        //             ParsedInputValue::Single(PrismaValue::List(list_value)) => {
        //                 list_args.push((field_name, list_value));
        //             },
        //             _ => unimplemented!(),
        //         }
        //     } else {
        //         match value {
        //             ParsedInputValue::Single(prisma_value) => {
        //                 non_list_args.insert(field_name, prisma_value);
        //             },
        //             _ => unimplemented!(),
        //         }
        //     }
        // }

        // let cr = CreateRecord{
        //     model: self.model,
        //     non_list_args: non_list_args,
        //     list_args: list_args,
        //     nested_writes: NestedWriteQueries::default(),
        // };
        // Ok(WriteQuery::Root(RootWriteQuery::CreateRecord(cr)))
        unimplemented!()
    }
}