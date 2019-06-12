#![warn(warnings)]

use crate::{
    builders::{build_nested_root, Operation, ValueMap, ValueSplit},
    CoreError, CoreResult,
};
use connector::mutaction::*;
use prisma_models::{Field as ModelField, ModelRef};

use std::sync::Arc;

pub struct SimpleNestedBuilder;

impl SimpleNestedBuilder {
    /// Build a set of nested value map mutations and attach them to an existing mutation level
    pub fn build(
        name: String,
        kind: String,
        map: ValueMap,
        mutations: &mut NestedMutactions,
        model: ModelRef,
        top_level: &Operation,
    ) -> CoreResult<()> {
        let name = name.as_str();
        let kind = kind.as_str();

        let where_ = map.to_node_selector(Arc::clone(dbg!(&model)));
        let ValueSplit { values, lists, nested } = map.split();

        let f = model.fields().find_from_all(&name);
        let (relation_field, relation_model) = match &f {
            Ok(ModelField::Relation(f)) => (Arc::clone(&f), f.related_model()),
            wat => panic!("Invalid state: `{:#?}`", wat),
        };

        let non_list_args = values.clone().to_prisma_values().into();
        let list_args = lists.into_iter().map(|la| la.convert()).collect();
        let nested_mutactions = build_nested_root(&name, &nested, Arc::clone(&relation_model), top_level)?;

        match kind {
            "create" => {
                mutations.creates.push(NestedCreateNode {
                    non_list_args,
                    list_args,
                    top_is_create: match top_level {
                        Operation::Create => true,
                        _ => false,
                    },
                    relation_field,
                    nested_mutactions,
                });
            }
            "delete" => {
                mutations.deletes.push(NestedDeleteNode { relation_field, where_ });
            }
            "connect" => {
                let where_ = values.to_node_selector(Arc::clone(&relation_model)).map_or(
                    Err(CoreError::QueryValidationError("No `where` on connect".into())),
                    |w| Ok(w),
                )?;
                mutations.connects.push(NestedConnect {
                    relation_field,
                    where_,
                    top_is_create: match top_level {
                        Operation::Create => true,
                        _ => false,
                    },
                });
            }
            "disconnect" => {
                let where_ = values.to_node_selector(Arc::clone(&relation_model));
                mutations.disconnects.push(NestedDisconnect { relation_field, where_ });
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
        mutations: &mut NestedMutactions,
        model: ModelRef,
        top_level: &Operation,
    ) -> CoreResult<()> {
        let name = name.as_str();
        let f = model.fields().find_from_all(&name);
        let (relation_field, related_model) = match &f {
            Ok(ModelField::Relation(f)) => (Arc::clone(&f), f.related_model()),
            wat => panic!("Invalid state: `{:#?}`", wat),
        };

        let where_ = dbg!(dbg!(&where_map).to_node_selector(Arc::clone(&related_model)));
        let create = {
            let ValueSplit { values, lists, nested } = create.split();
            let non_list_args = values.to_prisma_values().into();
            let list_args = lists.into_iter().map(|la| la.convert()).collect();
            let nested_mutactions = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), top_level)?;
            let relation_field = Arc::clone(&relation_field);

            NestedCreateNode {
                non_list_args,
                list_args,
                top_is_create: match top_level {
                    Operation::Create => true,
                    _ => false,
                },
                relation_field,
                nested_mutactions,
            }
        };

        let update = {
            let ValueSplit { values, lists, nested } = update.split();
            let non_list_args = values.to_prisma_values().into();
            let list_args = lists.into_iter().map(|la| la.convert()).collect();
            let nested_mutactions = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), top_level)?;
            let relation_field = Arc::clone(&relation_field);
            let where_ = where_.clone();

            NestedUpdateNode {
                relation_field,
                non_list_args,
                list_args,
                where_,
                nested_mutactions,
            }
        };

        mutations.upserts.push(NestedUpsertNode {
            relation_field,
            where_,
            create,
            update,
        });

        Ok(())
    }
}
