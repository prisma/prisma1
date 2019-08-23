use super::*;
use crate::query_builders::{ParsedInputMap, ParsedInputValue, QueryBuilderResult};
use connector::write_ast::*;
use prisma_models::{Field, ModelRef, PrismaArgs, PrismaListValue, PrismaValue};
use std::convert::TryInto;

#[derive(Default, Debug)]
pub struct WriteArguments {
    pub non_list: PrismaArgs,
    pub list: Vec<(String, PrismaListValue)>,
    pub nested: NestedWriteQueries,
}

impl WriteArguments {
    /// Creates a new set of WriteArguments from the `data` argument of a write query.
    /// Expects the parsed input map from the data key, not the enclosing map.
    ///
    /// Note: `root_create` is only a safeguard variable for now as we don't have flipped query semantics right now.
    /// This limitation will be lifted in the near future.
    pub fn from(model: &ModelRef, data_map: ParsedInputMap, triggered_from_create: bool) -> QueryBuilderResult<Self> {
        data_map.into_iter().try_fold(
            WriteArguments::default(),
            |mut args, (k, v): (String, ParsedInputValue)| {
                let field = model.fields().find_from_all(&k).unwrap();
                match field {
                    Field::Scalar(sf) if sf.is_list => {
                        let vals: ParsedInputMap = v.try_into()?;
                        let set_value: PrismaValue =
                            vals.into_iter().find(|(k, _)| k == "set").unwrap().1.try_into()?;
                        let list_value: PrismaListValue = set_value.try_into()?;

                        args.list.push((sf.name.clone(), list_value))
                    }

                    Field::Scalar(sf) => {
                        let value: PrismaValue = v.try_into()?;
                        args.non_list.insert(sf.name.clone(), value)
                    }

                    Field::Relation(ref rf) => {
                        args.nested
                            .merge(extract_nested_queries(rf, v.try_into()?, triggered_from_create)?);
                    }
                };

                Ok(args)
            },
        )
    }
}
