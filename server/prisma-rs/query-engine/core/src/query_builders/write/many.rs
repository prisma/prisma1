#![warn(warnings)]

use crate::{
    query_builders::{build_nested_root, utils, write::extend_defaults, ValueMap, ValueSplit},
    schema::OperationTag,
    CoreError, CoreResult,
};
use connector::write_ast::*;
use prisma_models::{Field, ModelRef, PrismaArgs, RelationFieldRef};
use std::sync::Arc;

pub struct ManyNestedBuilder;

impl ManyNestedBuilder {
    /// Build a set of nested value map writes and attach them to an existing write level
    pub fn build(
        name: String,
        kind: String,
        many: impl Iterator<Item = ValueMap>,
        write_queries: &mut NestedWriteQueries,
        model: ModelRef,
        top_level: OperationTag,
    ) -> CoreResult<()> {
        let name = name.as_str();
        let kind = kind.as_str();

        let field = model.fields().find_from_all(&name).unwrap();
        let (rel_field, rel_model) = match &field {
            Field::Relation(f) => (Arc::clone(&f), f.related_model()),
            _ => unimplemented!(),
        };

        for map in many.into_iter() {
            match kind {
                "create" => attach_create(name, map, write_queries, &rel_field, &rel_model, top_level)?,
                "connect" => attach_connect(map, write_queries, &rel_field, &rel_model, top_level)?,
                "disconnect" => attach_disconnect(map, write_queries, &model, &rel_field)?,
                "update" => attach_update(name, map, write_queries, &model, &rel_field, &rel_model, top_level)?,
                "updateMany" => attach_update_many(map, write_queries, &rel_field, &rel_model)?,
                "delete" => attach_delete(map, write_queries, &model, &rel_field)?,
                "deleteMany" => attach_delete_many(map, write_queries, &rel_field, &rel_model)?,
                verb => panic!("Invalid verb {:?}", verb),
            };
        }

        Ok(())
    }
}

fn attach_create(
    name: &str,
    map: ValueMap,
    nested_write_queries: &mut NestedWriteQueries,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
    top_level: OperationTag,
) -> CoreResult<()> {
    let ValueSplit { values, lists, nested } = map.split();
    let mut non_list_args = values.to_prisma_values();
    extend_defaults(&rel_model, &mut non_list_args);

    let mut non_list_args: PrismaArgs = non_list_args.into();
    non_list_args.add_datetimes(Arc::clone(&rel_model));

    let list_args = lists.into_iter().map(|la| la.convert()).collect();
    let nested_writes = build_nested_root(&name, &nested, Arc::clone(&rel_model), top_level)?;

    nested_write_queries.creates.push(NestedCreateRecord {
        non_list_args,
        list_args,
        top_is_create: match top_level {
            OperationTag::CreateOne => true,
            _ => false,
        },
        relation_field: Arc::clone(&rel_field),
        nested_writes,
    });

    Ok(())
}

fn attach_connect(
    map: ValueMap,
    nested_write_queries: &mut NestedWriteQueries,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
    top_level: OperationTag,
) -> CoreResult<()> {
    nested_write_queries.connects.push(NestedConnect {
        relation_field: Arc::clone(&rel_field),
        where_: map.to_record_finder(Arc::clone(&rel_model)).unwrap(),
        top_is_create: match top_level {
            OperationTag::CreateOne => true,
            _ => false,
        },
    });

    Ok(())
}

fn attach_disconnect(
    map: ValueMap,
    nested_write_queries: &mut NestedWriteQueries,
    model: &ModelRef,
    rel_field: &RelationFieldRef,
) -> CoreResult<()> {
    nested_write_queries.disconnects.push(NestedDisconnect {
        relation_field: Arc::clone(&rel_field),
        where_: map.to_record_finder(Arc::clone(&model)),
    });

    Ok(())
}

fn attach_update(
    name: &str,
    map: ValueMap,
    nested_write_queries: &mut NestedWriteQueries,
    model: &ModelRef,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
    top_level: OperationTag,
) -> CoreResult<()> {
    let where_ = map.to_record_finder(Arc::clone(&model));
    let ValueSplit { values, lists, nested } = map.split();

    let non_list_args = values.to_prisma_values().into();
    let list_args = lists.into_iter().map(|la| la.convert()).collect();
    let nested_writes = build_nested_root(&name, &nested, Arc::clone(&rel_model), top_level)?;

    nested_write_queries.updates.push(NestedUpdateRecord {
        relation_field: Arc::clone(&rel_field),
        non_list_args,
        list_args,
        where_,
        nested_writes,
    });

    Ok(())
}

fn attach_update_many(
    mut map: ValueMap,
    nested_write_queries: &mut NestedWriteQueries,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
) -> CoreResult<()> {
    let data = map.0.remove("data").map(|s| Ok(s)).unwrap_or_else(|| {
        Err(CoreError::LegacyQueryValidationError(
            "Malformed mutation: `data` section not found!".into(),
        ))
    })?;

    let filter = utils::extract_query_args_inner(
        map.0
            .iter()
            .filter(|(arg, _)| arg.as_str() != "data")
            .map(|(a, b)| (a, b)),
        Arc::clone(&rel_model),
    )?
    .filter;

    let ValueSplit {
        values,
        lists,
        nested: _,
    } = ValueMap::from(&data).split();
    let non_list_args = values.to_prisma_values().into();
    let list_args = lists.into_iter().map(|la| la.convert()).collect();

    nested_write_queries.update_manys.push(NestedUpdateManyRecords {
        relation_field: Arc::clone(&rel_field),
        filter,
        non_list_args,
        list_args,
    });
    Ok(())
}

fn attach_delete(
    map: ValueMap,
    nested_write_queries: &mut NestedWriteQueries,
    model: &ModelRef,
    rel_field: &RelationFieldRef,
) -> CoreResult<()> {
    nested_write_queries.deletes.push(NestedDeleteRecord {
        relation_field: Arc::clone(&rel_field),
        where_: map.to_record_finder(Arc::clone(&model)),
    });

    Ok(())
}

fn attach_delete_many(
    mut map: ValueMap,
    nested_write_queries: &mut NestedWriteQueries,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
) -> CoreResult<()> {
    let _ = map.0.remove("data").map(|s| Ok(s)).unwrap_or_else(|| {
        Err(CoreError::LegacyQueryValidationError(
            "Malformed mutation: `data` section not found!".into(),
        ))
    })?;

    let filter = utils::extract_query_args_inner(
        map.0
            .iter()
            .filter(|(arg, _)| arg.as_str() != "data")
            .map(|(a, b)| (a, b)),
        Arc::clone(&rel_model),
    )?
    .filter;

    nested_write_queries.delete_manys.push(NestedDeleteManyRecords {
        relation_field: Arc::clone(&rel_field),
        filter,
    });

    Ok(())
}
