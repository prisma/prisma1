#![warn(warnings)]

use crate::{
    builders::{build_nested_root, Operation, ValueMap, ValueSplit},
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

        let ValueSplit { values, lists, nested } = map.split();

        let field = model.fields().find_from_all(&name);
        let (relation_field, _) = match &field {
            Ok(Field::Relation(f)) => (Arc::clone(&f), f.related_model()),
            _ => unimplemented!(),
        };

        let model = Arc::clone(&relation_field.related_model());
        let non_list_args = values.to_prisma_values().into();
        let list_args = lists.into_iter().map(|la| la.convert()).collect();
        let nested_mutactions = build_nested_root(&name, &nested, model, top_level)?;

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
            _ => unimplemented!(),
        };

        Ok(())
    }
}
