use crate::query_builders::{Builder, ParsedField, ParsedInputMap, ParsedInputValue, QueryBuilderResult};
use connector::write_ast::{CreateRecord, NestedWriteQueries, RootWriteQuery, WriteQuery};
use prisma_models::{Field, ModelRef, PrismaArgs, PrismaListValue, PrismaValue, RelationFieldRef};
use std::{convert::TryInto, sync::Arc};

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
    fn build(self) -> QueryBuilderResult<WriteQuery> {
        let data_argument = self.field.arguments.into_iter().find(|arg| arg.name == "data").unwrap();
        let model = self.model;
        let data_map: ParsedInputMap = data_argument.value.try_into()?;
        let create_args = create_arguments(&model, data_map)?;

        // Todo generate nested queries

        let cr = CreateRecord {
            model: model,
            non_list_args: create_args.non_list,
            list_args: create_args.list,
            nested_writes: NestedWriteQueries::default(),
        };

        Ok(cr.into())
    }
}

#[derive(Default, Debug)]
struct SplitArguments {
    pub non_list: PrismaArgs,
    pub list: Vec<(String, PrismaListValue)>,
    pub nested: Vec<(RelationFieldRef, ParsedInputValue)>,
}

fn create_arguments(model: &ModelRef, data_map: ParsedInputMap) -> QueryBuilderResult<SplitArguments> {
    data_map.into_iter().try_fold(
        SplitArguments::default(),
        |mut args, (k, v): (String, ParsedInputValue)| {
            let field = model.fields().find_from_all(&k).unwrap();
            match field {
                Field::Scalar(sf) if sf.is_list => {
                    let vals: ParsedInputMap = v.try_into()?;
                    let set_value: PrismaValue = vals.into_iter().find(|(k, _)| k == "set").unwrap().1.try_into()?;
                    let list_value: PrismaListValue = set_value.try_into()?;

                    args.list.push((sf.name.clone(), list_value))
                }

                Field::Scalar(sf) => {
                    let value: PrismaValue = v.try_into()?;
                    args.non_list.insert(sf.name.clone(), value)
                }

                Field::Relation(ref rf) => {
                    args.nested.push((Arc::clone(rf), v));
                }
            };

            Ok(args)
        },
    )
}
