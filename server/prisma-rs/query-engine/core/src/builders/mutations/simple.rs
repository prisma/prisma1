#![warn(warnings)]

use crate::{
    builders::{build_nested_root, Operation, ValueMap, ValueSplit},
    CoreResult,
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

        let where_ = map.to_node_selector(Arc::clone(&model));
        let ValueSplit { values, lists, nested } = map.split();

        let f = model.fields().find_from_all(&name);
        let (relation_field, relation_model) = match &f {
            Ok(ModelField::Relation(f)) => (Arc::clone(&f), f.related_model()),
            wat => panic!("{:#?}", wat),
        };

        let non_list_args = values.to_prisma_values().into();
        let list_args = lists.into_iter().map(|la| la.convert()).collect();
        let nested_mutactions = build_nested_root(&name, &nested, relation_model, top_level)?;

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
            _ => unimplemented!(),
        };

        Ok(())
    }
}
