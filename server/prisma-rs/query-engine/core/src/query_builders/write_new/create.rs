use crate::query_builders::{
    extract_filter, utils, Builder, ParsedField, ParsedInputMap, ParsedInputValue, QueryBuilderResult,
};
use connector::{
    filter::{Filter, RecordFinder},
    write_ast::*,
};
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
        let data_argument = self
            .field
            .arguments
            .into_iter()
            .find(|arg| arg.name == "data")
            .expect("");
        let model = self.model;
        let data_map: ParsedInputMap = data_argument.value.try_into()?;
        let create_args = WriteArguments::from(&model, data_map, true)?;
        let cr = CreateRecord {
            model: model,
            non_list_args: create_args.non_list,
            list_args: create_args.list,
            nested_writes: create_args.nested,
        };

        Ok(cr.into())
    }
}

#[derive(Default, Debug)]
struct WriteArguments {
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
    fn from(model: &ModelRef, data_map: ParsedInputMap, triggered_from_create: bool) -> QueryBuilderResult<Self> {
        data_map.into_iter().try_fold(
            WriteArguments::default(),
            |mut args, (k, v): (String, ParsedInputValue)| {
                let field = model.fields().find_from_all(&k).expect("");
                match field {
                    Field::Scalar(sf) if sf.is_list => {
                        let vals: ParsedInputMap = v.try_into()?;
                        let set_value: PrismaValue =
                            vals.into_iter().find(|(k, _)| k == "set").expect("").1.try_into()?;
                        let list_value: PrismaListValue = set_value.try_into()?;

                        args.list.push((sf.name.clone(), list_value))
                    }

                    Field::Scalar(sf) => {
                        let value: PrismaValue = v.try_into()?;
                        args.non_list.insert(sf.name.clone(), value)
                    }

                    Field::Relation(ref rf) => {
                        args.nested
                            .merge(Self::extract_nested_queries(rf, v.try_into()?, triggered_from_create)?);
                    }
                };

                Ok(args)
            },
        )
    }

    fn extract_nested_queries(
        relation_field: &RelationFieldRef,
        field: ParsedInputMap,
        triggered_from_create: bool,
    ) -> QueryBuilderResult<NestedWriteQueries> {
        let model = relation_field.related_model();

        field
            .into_iter()
            .fold(Ok(NestedWriteQueries::default()), |prev, (name, value)| {
                let mut prev = prev?;
                match name.as_str() {
                    "create" => {
                        Self::nested_create(value, &model, &relation_field, triggered_from_create)?
                            .into_iter()
                            .for_each(|nested_create| prev.creates.push(nested_create));
                    }
                    "update" => {
                        Self::nested_update(value, &model, &relation_field, triggered_from_create)?
                            .into_iter()
                            .for_each(|nested_update| prev.updates.push(nested_update));
                    }
                    "upsert" => {
                        Self::nested_upsert(value, &model, &relation_field, triggered_from_create)?
                            .into_iter()
                            .for_each(|nested_upsert| prev.upserts.push(nested_upsert));
                    }
                    "delete" => {
                        Self::nested_delete(value, &model, &relation_field)?
                            .into_iter()
                            .for_each(|nested_delete| prev.deletes.push(nested_delete));
                    }
                    "connect" => {
                        Self::nested_connect(value, &model, &relation_field, triggered_from_create)?
                            .into_iter()
                            .for_each(|nested_connect| prev.connects.push(nested_connect));
                    }
                    "set" => {
                        Self::nested_set(value, &model, &relation_field)?
                            .into_iter()
                            .for_each(|nested_set| prev.sets.push(nested_set));
                    }
                    "disconnect" => {
                        Self::nested_disconnect(value, &model, &relation_field)?
                            .into_iter()
                            .for_each(|nested_disconnect| prev.disconnects.push(nested_disconnect));
                    }
                    "updateMany" => {
                        Self::nested_update_many(value, &model, &relation_field)?
                            .into_iter()
                            .for_each(|nested_update_many| prev.update_manys.push(nested_update_many));
                    }
                    "deleteMany" => {
                        Self::nested_delete_many(value, &model, &relation_field)?
                            .into_iter()
                            .for_each(|nested_delete_many| prev.delete_manys.push(nested_delete_many));
                    }
                    _ => unimplemented!(),
                };

                Ok(prev)
            })
    }

    fn nested_create(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
        triggered_from_create: bool,
    ) -> QueryBuilderResult<Vec<NestedCreateRecord>> {
        Self::coerce_vec(value)
            .into_iter()
            .map(|value| {
                let args = WriteArguments::from(&model, value.try_into()?, true)?;

                Ok(NestedCreateRecord {
                    relation_field: Arc::clone(relation_field),
                    non_list_args: args.non_list,
                    list_args: args.list,
                    nested_writes: args.nested,
                    top_is_create: triggered_from_create,
                })
            })
            .collect::<QueryBuilderResult<Vec<_>>>()
    }

    fn nested_update(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
        triggered_from_create: bool,
    ) -> QueryBuilderResult<Vec<NestedUpdateRecord>> {
        Self::coerce_vec(value)
            .into_iter()
            .map(|value| {
                let mut map: ParsedInputMap = value.try_into()?;
                let data_arg = map.remove("data").expect("1");
                let write_args = WriteArguments::from(&model, data_arg.try_into()?, false)?;

                let record_finder = if relation_field.is_list {
                    let where_arg = map.remove("where").expect("2");
                    Some(utils::extract_record_finder(where_arg, &model)?)
                } else {
                    None
                };

                Ok(NestedUpdateRecord {
                    relation_field: Arc::clone(&relation_field),
                    where_: record_finder,
                    non_list_args: write_args.non_list,
                    list_args: write_args.list,
                    nested_writes: write_args.nested,
                })
            })
            .collect::<QueryBuilderResult<Vec<_>>>()
    }

    fn nested_upsert(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
        triggered_from_create: bool,
    ) -> QueryBuilderResult<Vec<NestedUpsertRecord>> {
        Self::coerce_vec(value)
            .into_iter()
            .map(|value| {
                let mut map: ParsedInputMap = value.try_into()?;
                let create_arg = map.remove("create").expect("3");
                let update_arg = map.remove("update").expect("4");
                let mut create = Self::nested_create(create_arg, model, relation_field, triggered_from_create)?;
                let mut update = Self::nested_update(update_arg, model, relation_field, triggered_from_create)?;

                let record_finder = if relation_field.is_list {
                    let where_arg = map.remove("where").expect("5");
                    Some(utils::extract_record_finder(where_arg, &model)?)
                } else {
                    None
                };

                Ok(NestedUpsertRecord {
                    relation_field: Arc::clone(&relation_field),
                    where_: record_finder,
                    create: create.pop().unwrap(),
                    update: update.pop().unwrap(),
                })
            })
            .collect::<QueryBuilderResult<Vec<_>>>()
    }

    fn nested_delete(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
    ) -> QueryBuilderResult<Vec<NestedDeleteRecord>> {
        Self::coerce_vec(value)
            .into_iter()
            .map(|value| {
                let mut map: ParsedInputMap = value.try_into()?;
                let record_finder = if relation_field.is_list {
                    let where_arg = map.remove("where").expect("7");
                    Some(utils::extract_record_finder(where_arg, &model)?)
                } else {
                    None
                };

                Ok(NestedDeleteRecord {
                    relation_field: Arc::clone(&relation_field),
                    where_: record_finder,
                })
            })
            .collect::<QueryBuilderResult<Vec<_>>>()
    }

    fn nested_connect(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
        triggered_from_create: bool,
    ) -> QueryBuilderResult<Vec<NestedConnect>> {
        Self::coerce_vec(value)
            .into_iter()
            .map(|value| {
                let record_finder = utils::extract_record_finder(value, &model)?;

                Ok(NestedConnect {
                    relation_field: Arc::clone(&relation_field),
                    where_: record_finder,
                    top_is_create: triggered_from_create,
                })
            })
            .collect::<QueryBuilderResult<Vec<_>>>()
    }

    fn nested_set(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
    ) -> QueryBuilderResult<Vec<NestedSet>> {
        let finders = Self::coerce_vec(value)
            .into_iter()
            .map(|value| utils::extract_record_finder(value, &model))
            .collect::<QueryBuilderResult<Vec<_>>>()?;

        Ok(vec![NestedSet {
            relation_field: Arc::clone(&relation_field),
            wheres: finders,
        }])
    }

    fn nested_disconnect(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
    ) -> QueryBuilderResult<Vec<NestedDisconnect>> {
        Self::coerce_vec(value)
            .into_iter()
            .map(|value| {
                let mut map: ParsedInputMap = value.try_into()?;
                let record_finder = if relation_field.is_list {
                    let where_arg = map.remove("where").expect("asd");
                    Some(utils::extract_record_finder(where_arg, &model)?)
                } else {
                    None
                };

                Ok(NestedDisconnect {
                    relation_field: Arc::clone(&relation_field),
                    where_: record_finder,
                })
            })
            .collect::<QueryBuilderResult<Vec<_>>>()
    }

    fn nested_update_many(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
    ) -> QueryBuilderResult<Vec<NestedUpdateManyRecords>> {
        Self::coerce_vec(value)
            .into_iter()
            .map(|value| {
                let mut map: ParsedInputMap = value.try_into()?;
                let data_arg = map.remove("data").expect("123");
                let write_args = WriteArguments::from(&model, data_arg.try_into()?, false)?;

                let filter = if relation_field.is_list {
                    let where_arg = map.remove("where").expect("sss");
                    Some(extract_filter(where_arg.try_into()?, &model)?)
                } else {
                    None
                };

                Ok(NestedUpdateManyRecords {
                    relation_field: Arc::clone(&relation_field),
                    filter,
                    non_list_args: write_args.non_list,
                    list_args: write_args.list,
                })
            })
            .collect::<QueryBuilderResult<Vec<_>>>()
    }

    fn nested_delete_many(
        value: ParsedInputValue,
        model: &ModelRef,
        relation_field: &RelationFieldRef,
    ) -> QueryBuilderResult<Vec<NestedDeleteManyRecords>> {
        Self::coerce_vec(value)
            .into_iter()
            .map(|value| {
                let mut map: ParsedInputMap = value.try_into()?;
                let filter = if relation_field.is_list {
                    let where_arg = map.remove("where").expect("asdasda");
                    Some(extract_filter(where_arg.try_into()?, &model)?)
                } else {
                    None
                };

                Ok(NestedDeleteManyRecords {
                    relation_field: Arc::clone(&relation_field),
                    filter,
                })
            })
            .collect::<QueryBuilderResult<Vec<_>>>()
    }

    fn coerce_vec(val: ParsedInputValue) -> Vec<ParsedInputValue> {
        match val {
            ParsedInputValue::List(l) => l,
            m @ ParsedInputValue::Map(_) => vec![m],
            _ => unreachable!(),
        }
    }
}
