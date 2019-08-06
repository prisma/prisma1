use super::*;
use crate::query_builders::{
    extract_filter, utils, ParsedInputMap, ParsedInputValue, QueryBuilderResult, QueryValidationError,
};
use connector::write_ast::*;
use prisma_models::{ModelRef, PrismaValue, RelationFieldRef, RelationLinkManifestation};
use std::{convert::TryInto, sync::Arc};

pub fn extract_nested_queries(
    relation_field: &RelationFieldRef,
    field: ParsedInputMap,
    triggered_from_create: bool,
) -> QueryBuilderResult<NestedWriteQueries> {
    let model = relation_field.related_model();

    let nested_queries = field
        .into_iter()
        .fold(Ok(NestedWriteQueries::default()), |prev, (name, value)| {
            let mut prev = prev?;
            match name.as_str() {
                "create" => {
                    nested_create(value, &model, &relation_field, triggered_from_create)?
                        .into_iter()
                        .for_each(|nested_create| prev.creates.push(nested_create));
                }
                "update" => {
                    nested_update(value, &model, &relation_field)?
                        .into_iter()
                        .for_each(|nested_update| prev.updates.push(nested_update));
                }
                "upsert" => {
                    nested_upsert(value, &model, &relation_field, triggered_from_create)?
                        .into_iter()
                        .for_each(|nested_upsert| prev.upserts.push(nested_upsert));
                }
                "delete" => {
                    nested_delete(value, &model, &relation_field)?
                        .into_iter()
                        .for_each(|nested_delete| prev.deletes.push(nested_delete));
                }
                "connect" => {
                    nested_connect(value, &model, &relation_field, triggered_from_create)?
                        .into_iter()
                        .for_each(|nested_connect| prev.connects.push(nested_connect));
                }
                "set" => {
                    nested_set(value, &model, &relation_field)?
                        .into_iter()
                        .for_each(|nested_set| prev.sets.push(nested_set));
                }
                "disconnect" => {
                    nested_disconnect(value, &model, &relation_field)?
                        .into_iter()
                        .for_each(|nested_disconnect| prev.disconnects.push(nested_disconnect));
                }
                "updateMany" => {
                    nested_update_many(value, &model, &relation_field)?
                        .into_iter()
                        .for_each(|nested_update_many| prev.update_manys.push(nested_update_many));
                }
                "deleteMany" => {
                    nested_delete_many(value, &model, &relation_field)?
                        .into_iter()
                        .for_each(|nested_delete_many| prev.delete_manys.push(nested_delete_many));
                }
                _ => unimplemented!(),
            };

            Ok(prev)
        });

    // Check if we need to abort due to limitation in the query engine
    nested_queries.and_then(|nq| {
        let is_unsupported = match relation_field.relation().manifestation {
            Some(RelationLinkManifestation::Inline(ref i)) => {
                i.in_table_of_model_name == relation_field.model().db_name()
            }
            _ => false,
        };

        let is_local = std::env::var("SERVER_ROOT").is_ok();
        if triggered_from_create
            && (nq.creates.len() + nq.connects.len() > 0)
            && relation_field.is_required
            && is_unsupported
            && !is_local
        {
            Err(QueryValidationError::AssertionError(
                format!(
                    "A create or connect cannot be performed in this direction. You're first creating a {} and then {}, but only the reverse order is currently supported. This limitation will be lifted for general availability",
                    relation_field.model().name,
                    relation_field.related_model().name)
                )
            )
        } else {
            Ok(nq)
        }
    })
}

pub fn nested_create(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
    triggered_from_create: bool,
) -> QueryBuilderResult<Vec<NestedCreateRecord>> {
    coerce_vec(value)
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

pub fn nested_update(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
) -> QueryBuilderResult<Vec<NestedUpdateRecord>> {
    let mut vec = vec![];

    for value in coerce_vec(value) {
        if relation_field.is_list {
            let mut map: ParsedInputMap = value.try_into()?;
            let data_arg = map.remove("data").expect("1");
            let write_args = WriteArguments::from(&model, data_arg.try_into()?, false)?;
            let where_arg = map.remove("where").expect("2");
            let record_finder = Some(utils::extract_record_finder(where_arg, &model)?);

            vec.push(NestedUpdateRecord {
                relation_field: Arc::clone(&relation_field),
                where_: record_finder,
                non_list_args: write_args.non_list,
                list_args: write_args.list,
                nested_writes: write_args.nested,
            });
        } else {
            let write_args = WriteArguments::from(&model, value.try_into()?, false)?;

            vec.push(NestedUpdateRecord {
                relation_field: Arc::clone(&relation_field),
                where_: None,
                non_list_args: write_args.non_list,
                list_args: write_args.list,
                nested_writes: write_args.nested,
            });
        }
    }

    Ok(vec)
}

pub fn nested_upsert(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
    triggered_from_create: bool,
) -> QueryBuilderResult<Vec<NestedUpsertRecord>> {
    coerce_vec(value)
        .into_iter()
        .map(|value| {
            let mut map: ParsedInputMap = value.try_into()?;
            let record_finder = if relation_field.is_list {
                let where_arg = map.remove("where").expect("5");
                Some(utils::extract_record_finder(where_arg, &model)?)
            } else {
                None
            };

            let create_arg = map.remove("create").expect("3");
            let create_args = WriteArguments::from(&model, create_arg.try_into()?, triggered_from_create)?;

            let update_arg = map.remove("update").expect("4");
            let update_args = WriteArguments::from(&model, update_arg.try_into()?, triggered_from_create)?;

            let create = NestedCreateRecord {
                relation_field: Arc::clone(relation_field),
                non_list_args: create_args.non_list,
                list_args: create_args.list,
                nested_writes: create_args.nested,
                top_is_create: triggered_from_create,
            };

            let update = NestedUpdateRecord {
                relation_field: Arc::clone(&relation_field),
                where_: record_finder.clone(),
                non_list_args: update_args.non_list,
                list_args: update_args.list,
                nested_writes: update_args.nested,
            };

            Ok(NestedUpsertRecord {
                relation_field: Arc::clone(&relation_field),
                where_: record_finder,
                create,
                update,
            })
        })
        .collect::<QueryBuilderResult<Vec<_>>>()
}

pub fn nested_delete(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
) -> QueryBuilderResult<Vec<NestedDeleteRecord>> {
    let mut vec = vec![];

    for value in coerce_vec(value) {
        if relation_field.is_list {
            let record_finder = if relation_field.is_list {
                Some(utils::extract_record_finder(value, &model)?)
            } else {
                None
            };

            vec.push(NestedDeleteRecord {
                relation_field: Arc::clone(&relation_field),
                where_: record_finder,
            })
        } else {
            let val: PrismaValue = value.try_into()?;
            match val {
                PrismaValue::Boolean(b) if b => vec.push(NestedDeleteRecord {
                    relation_field: Arc::clone(&relation_field),
                    where_: None,
                }),
                _ => (),
            };
        }
    }

    Ok(vec)
}

pub fn nested_connect(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
    triggered_from_create: bool,
) -> QueryBuilderResult<Vec<NestedConnect>> {
    coerce_vec(value)
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

pub fn nested_set(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
) -> QueryBuilderResult<Vec<NestedSet>> {
    let finders = coerce_vec(value)
        .into_iter()
        .map(|value| utils::extract_record_finder(value, &model))
        .collect::<QueryBuilderResult<Vec<_>>>()?;

    Ok(vec![NestedSet {
        relation_field: Arc::clone(&relation_field),
        wheres: finders,
    }])
}

pub fn nested_disconnect(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
) -> QueryBuilderResult<Vec<NestedDisconnect>> {
    let mut vec = vec![];

    for value in coerce_vec(value) {
        if relation_field.is_list {
            let record_finder = if relation_field.is_list {
                Some(utils::extract_record_finder(value, &model)?)
            } else {
                None
            };

            vec.push(NestedDisconnect {
                relation_field: Arc::clone(&relation_field),
                where_: record_finder,
            })
        } else {
            let val: PrismaValue = value.try_into()?;
            match val {
                PrismaValue::Boolean(b) if b => vec.push(NestedDisconnect {
                    relation_field: Arc::clone(&relation_field),
                    where_: None,
                }),
                _ => (),
            };
        }
    }

    Ok(vec)
}

pub fn nested_update_many(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
) -> QueryBuilderResult<Vec<NestedUpdateManyRecords>> {
    coerce_vec(value)
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

pub fn nested_delete_many(
    value: ParsedInputValue,
    model: &ModelRef,
    relation_field: &RelationFieldRef,
) -> QueryBuilderResult<Vec<NestedDeleteManyRecords>> {
    coerce_vec(value)
        .into_iter()
        .map(|value| {
            // Note: how can a *_many nested mutation be without a list?
            let filter = if relation_field.is_list {
                Some(extract_filter(value.try_into()?, &model)?)
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

pub fn coerce_vec(val: ParsedInputValue) -> Vec<ParsedInputValue> {
    match val {
        ParsedInputValue::List(l) => l,
        m @ ParsedInputValue::Map(_) => vec![m],
        single => vec![single],
    }
}
