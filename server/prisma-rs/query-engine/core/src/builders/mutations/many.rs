#![warn(warnings)]

use crate::{
    builders::{build_nested_root, utils, Operation, ValueMap, ValueSplit},
    CoreError, CoreResult,
};
use connector::mutaction::*;
use prisma_models::{Field, ModelRef, RelationFieldRef};

use std::sync::Arc;

pub struct ManyNestedBuilder;

impl ManyNestedBuilder {
    /// Build a set of nested value map mutations and attach them to an existing mutation level
    pub fn build(
        name: String,
        kind: String,
        many: impl Iterator<Item = ValueMap>,
        mutations: &mut NestedMutactions,
        model: ModelRef,
        top_level: &Operation,
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
                "create" => attach_create(name, map, mutations, &rel_field, &rel_model, top_level)?,
                "connect" => attach_connect(map, mutations, &rel_field, &rel_model, top_level)?,
                "disconnect" => attach_disconnect(map, mutations, &model, &rel_field)?,
                "update" => attach_update(name, map, mutations, &model, &rel_field, &rel_model, top_level)?,
                "updateMany" => attach_update_many(map, mutations, &rel_field, &rel_model)?,
                "delete" => attach_delete(map, mutations, &model, &rel_field)?,
                "deleteMany" => attach_delete_many(map, mutations, &rel_field, &rel_model)?,
                verb => panic!("huh? {:?}", verb),
            };
        }

        Ok(())
    }
}

fn attach_create(
    name: &str,
    map: ValueMap,
    mutations: &mut NestedMutactions,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
    top_level: &Operation,
) -> CoreResult<()> {
    let ValueSplit { values, lists, nested } = map.split();
    let non_list_args = values.to_prisma_values().into();
    let list_args = lists.into_iter().map(|la| la.convert()).collect();
    let nested_mutactions = build_nested_root(&name, &nested, Arc::clone(&rel_model), top_level)?;

    mutations.creates.push(NestedCreateNode {
        non_list_args,
        list_args,
        top_is_create: match top_level {
            Operation::Create => true,
            _ => false,
        },
        relation_field: Arc::clone(&rel_field),
        nested_mutactions,
    });

    Ok(())
}

fn attach_connect(
    map: ValueMap,
    mutations: &mut NestedMutactions,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
    top_level: &Operation,
) -> CoreResult<()> {
    mutations.connects.push(NestedConnect {
        relation_field: Arc::clone(&rel_field),
        where_: map.to_node_selector(Arc::clone(&rel_model)).unwrap(),
        top_is_create: match top_level {
            Operation::Create => true,
            _ => false,
        },
    });

    Ok(())
}

fn attach_disconnect(
    map: ValueMap,
    mutations: &mut NestedMutactions,
    model: &ModelRef,
    rel_field: &RelationFieldRef,
) -> CoreResult<()> {
    mutations.disconnects.push(NestedDisconnect {
        relation_field: Arc::clone(&rel_field),
        where_: map.to_node_selector(Arc::clone(&model)),
    });

    Ok(())
}

fn attach_update(
    name: &str,
    map: ValueMap,
    mutations: &mut NestedMutactions,
    model: &ModelRef,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
    top_level: &Operation,
) -> CoreResult<()> {
    let where_ = map.to_node_selector(Arc::clone(&model));
    let ValueSplit { values, lists, nested } = map.split();

    let non_list_args = values.to_prisma_values().into();
    let list_args = lists.into_iter().map(|la| la.convert()).collect();
    let nested_mutactions = build_nested_root(&name, &nested, Arc::clone(&rel_model), top_level)?;

    mutations.updates.push(NestedUpdateNode {
        relation_field: Arc::clone(&rel_field),
        non_list_args,
        list_args,
        where_,
        nested_mutactions,
    });

    Ok(())
}

fn attach_update_many(
    mut map: ValueMap,
    mutations: &mut NestedMutactions,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
) -> CoreResult<()> {
    let data = map.0.remove("data").map(|s| Ok(s)).unwrap_or_else(|| {
        Err(CoreError::QueryValidationError(
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

    mutations.update_manys.push(NestedUpdateNodes {
        relation_field: Arc::clone(&rel_field),
        filter,
        non_list_args,
        list_args,
    });
    Ok(())
}

fn attach_delete(
    map: ValueMap,
    mutations: &mut NestedMutactions,
    model: &ModelRef,
    rel_field: &RelationFieldRef,
) -> CoreResult<()> {
    mutations.deletes.push(NestedDeleteNode {
        relation_field: Arc::clone(&rel_field),
        where_: map.to_node_selector(Arc::clone(&model)),
    });

    Ok(())
}

fn attach_delete_many(
    mut map: ValueMap,
    mutations: &mut NestedMutactions,
    rel_field: &RelationFieldRef,
    rel_model: &ModelRef,
) -> CoreResult<()> {
    let _ = map.0.remove("data").map(|s| Ok(s)).unwrap_or_else(|| {
        Err(CoreError::QueryValidationError(
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

    mutations.delete_manys.push(NestedDeleteNodes {
        relation_field: Arc::clone(&rel_field),
        filter,
    });

    Ok(())
}
