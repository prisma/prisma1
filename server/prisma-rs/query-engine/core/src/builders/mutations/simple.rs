#![warn(warnings)]

use crate::{
    builders::{build_nested_root, utils, Operation, ValueMap, ValueSplit},
    CoreResult,
};
use connector::mutaction::*;
use prisma_models::{Field, ModelRef};

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

        let field = model.fields().find_from_all(&name);
        let (relation_field, relation_model) = match &field {
            Ok(Field::Relation(f)) => (Arc::clone(&f), f.related_model()),
            wat => panic!("Unimplemented `{:?}`", wat),
        };

        let where_ = map.to_node_selector(Arc::clone(&model)).ok();
        let filter = utils::extract_query_args_inner(
            map.0
                .iter()
                .filter(|(arg, _)| arg.as_str() != "data")
                .map(|(a, b)| (a, b)),
            Arc::clone(&relation_model),
        )?
        .filter;

        let ValueSplit { values, lists, nested } = map.split();

        let model = Arc::clone(&relation_field.related_model());
        let non_list_args = values.to_prisma_values().into();
        let list_args = lists.into_iter().map(|la| la.convert()).collect();
        let nested_mutactions = build_nested_root(&name, &nested, model, top_level)?;
        let top_is_create = match top_level {
            Operation::Create => true,
            _ => false,
        };

        match kind {
            "create" => {
                mutations.creates.push(NestedCreateNode {
                    non_list_args,
                    list_args,
                    top_is_create,
                    relation_field,
                    nested_mutactions,
                });
            }
            "update" => {
                mutations.updates.push(NestedUpdateNode {
                    where_,
                    non_list_args,
                    list_args,
                    relation_field,
                    nested_mutactions,
                });
            }
            "updateMany" => {
                mutations.update_manys.push(NestedUpdateNodes {
                    relation_field,
                    filter,
                    non_list_args,
                    list_args,
                });
            }
            "delete" => {
                mutations.deletes.push(NestedDeleteNode { relation_field, where_ });
            }
            "deleteMany" => {
                mutations
                    .delete_manys
                    .push(NestedDeleteNodes { relation_field, filter });
            }
            "upsert" => {
                mutations.upserts.push(NestedUpsertNode {
                    relation_field: Arc::clone(&relation_field),
                    where_: where_.clone(),
                    create: NestedCreateNode {
                        non_list_args: non_list_args.clone(),
                        list_args: list_args.clone(),
                        top_is_create,
                        relation_field: Arc::clone(&relation_field),
                        nested_mutactions: nested_mutactions.clone(),
                    },
                    update: NestedUpdateNode {
                        where_: where_.clone(),
                        non_list_args: non_list_args.clone(),
                        list_args: list_args.clone(),
                        relation_field: Arc::clone(&relation_field),
                        nested_mutactions: nested_mutactions.clone(),
                    },
                });
            }
            "connect" => {
                mutations.connects.push(NestedConnect {
                    relation_field,
                    where_: where_.unwrap(),
                    top_is_create: match top_level {
                        Operation::Create => true,
                        _ => false,
                    },
                });
            }
            "disconnect" => {
                mutations.disconnects.push(NestedDisconnect { relation_field, where_ });
            }
            _ => unimplemented!(),
        };

        Ok(())
    }
}
