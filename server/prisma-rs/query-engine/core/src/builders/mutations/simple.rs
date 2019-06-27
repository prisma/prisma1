#![warn(warnings)]

use crate::{
    builders::{build_nested_root, utils, ValueMap, ValueSplit},
    extend_defaults,
    schema::OperationTag,
    CoreError, CoreResult,
};
use connector::write_query::*;
use prisma_models::{Field as ModelField, ModelRef, PrismaArgs};
use std::sync::Arc;

pub struct SimpleNestedBuilder;

impl SimpleNestedBuilder {
    /// Build a set of nested value map writes and attach them to an existing write level
    pub fn build(
        name: String,
        kind: String,
        map: ValueMap,
        nested_write_queries: &mut NestedWriteQueries,
        model: ModelRef,
        where_map: Option<ValueMap>,
        top_level: OperationTag,
    ) -> CoreResult<()> {
        let name = name.as_str();
        let kind = kind.as_str();
        let where_ = where_map
            .as_ref()
            .or(Some(&map))
            .and_then(|m| m.to_record_finder(Arc::clone(&model)));

        let ValueSplit { values, lists, nested } = map.split();
        let f = model.fields().find_from_all(&name);
        let (relation_field, related_model) = match &f {
            Ok(ModelField::Relation(f)) => (Arc::clone(&f), f.related_model()),
            wat => panic!("Invalid state: `{:#?}`", wat),
        };

        println!("FOOBAR {:?} {:?} {:?}", f, relation_field, related_model);

        let mut non_list_args = values.clone().to_prisma_values();
        let list_args = lists.into_iter().map(|la| la.convert()).collect();
        let nested_writes = build_nested_root(&name, &nested, Arc::clone(&related_model), top_level)?;

        match kind {
            "create" => {
                extend_defaults(&model, &mut non_list_args);

                let mut non_list_args: PrismaArgs = non_list_args.into();
                non_list_args.add_datetimes(Arc::clone(&model));

                nested_write_queries.creates.push(NestedCreateRecord {
                    non_list_args,
                    list_args,
                    top_is_create: match top_level {
                        OperationTag::CreateOne => true,
                        _ => false,
                    },
                    relation_field,
                    nested_writes,
                });
            }
            "delete" => {
                nested_write_queries
                    .deletes
                    .push(NestedDeleteRecord { relation_field, where_ });
            }
            "connect" => {
                let where_ = values.to_record_finder(Arc::clone(&related_model)).map_or(
                    Err(CoreError::QueryValidationError("No `where` on connect".into())),
                    |w| Ok(w),
                )?;
                nested_write_queries.connects.push(NestedConnect {
                    relation_field,
                    where_,
                    top_is_create: match top_level {
                        OperationTag::CreateOne => true,
                        _ => false,
                    },
                });
            }
            "disconnect" => {
                let where_ = values.to_record_finder(Arc::clone(&related_model));
                nested_write_queries
                    .disconnects
                    .push(NestedDisconnect { relation_field, where_ });
            }
            "update" => {
                nested_write_queries.updates.push(NestedUpdateRecord {
                    relation_field,
                    non_list_args: non_list_args.into(),
                    list_args,
                    where_,
                    nested_writes,
                });
            }
            "updateMany" => {
                use graphql_parser::query::Value;
                use std::collections::BTreeMap;
                let mut wheree: BTreeMap<String, Value> = BTreeMap::new();
                wheree.insert(
                    "where".into(),
                    Value::Object(
                        where_map
                            .map_or(
                                Err(CoreError::QueryValidationError("Failed to read `where` block".into())),
                                |w| Ok(w),
                            )?
                            .0,
                    ),
                );

                let filter =
                    utils::extract_query_args_inner(wheree.iter().map(|(a, b)| (a, b)), Arc::clone(&related_model))?
                        .filter;

                nested_write_queries.update_manys.push(NestedUpdateManyRecords {
                    relation_field,
                    filter,
                    non_list_args: non_list_args.into(),
                    list_args,
                });
            }
            _ => unimplemented!(),
        };

        Ok(())
    }
}

pub struct UpsertNestedBuilder;

impl UpsertNestedBuilder {
    pub fn build(
        name: String,
        where_map: ValueMap,
        create: ValueMap,
        update: ValueMap,
        nested_write_queries: &mut NestedWriteQueries,
        model: ModelRef,
        top_level: OperationTag,
    ) -> CoreResult<()> {
        let name = name.as_str();
        let f = model.fields().find_from_all(&name);
        let (relation_field, related_model) = match &f {
            Ok(ModelField::Relation(f)) => (Arc::clone(&f), f.related_model()),
            wat => panic!("Invalid state: `{:#?}`", wat),
        };

        let where_ = where_map.to_record_finder(Arc::clone(&related_model));
        let create = {
            let ValueSplit { values, lists, nested } = create.split();
            let mut non_list_args = values.to_prisma_values();
            extend_defaults(&model, &mut non_list_args);

            let mut non_list_args: PrismaArgs = non_list_args.into();
            non_list_args.add_datetimes(Arc::clone(&model));

            let list_args = lists.into_iter().map(|la| la.convert()).collect();
            let nested_writes = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), top_level)?;
            let relation_field = Arc::clone(&relation_field);

            NestedCreateRecord {
                non_list_args,
                list_args,
                top_is_create: match top_level {
                    OperationTag::CreateOne => true,
                    _ => false,
                },
                relation_field,
                nested_writes,
            }
        };

        let update = {
            let ValueSplit { values, lists, nested } = update.split();
            let non_list_args = values.to_prisma_values().into();
            let list_args = lists.into_iter().map(|la| la.convert()).collect();
            let nested_writes = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), top_level)?;
            let relation_field = Arc::clone(&relation_field);
            let where_ = where_.clone();

            NestedUpdateRecord {
                relation_field,
                non_list_args,
                list_args,
                where_,
                nested_writes,
            }
        };

        nested_write_queries.upserts.push(NestedUpsertRecord {
            relation_field,
            where_,
            create,
            update,
        });

        Ok(())
    }
}
