use super::NestedMutactions;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct CreateNode {
    pub model: ModelRef,
    pub non_list_args: PrismaArgs,
    pub list_args: PrismaArgs,
    pub nested_mutactions: NestedMutactions,
}

#[derive(Debug, Clone)]
pub struct NestedCreateNode {
    pub model: ModelRef,
    pub non_list_args: PrismaArgs,
    pub list_args: PrismaArgs,
    pub nested_mutactions: NestedMutactions,

    pub relation_field: Arc<RelationField>,
    pub top_is_create: bool,
}

impl CreateNode {
    pub fn new(
        model: ModelRef,
        non_list_args: PrismaArgs,
        list_args: PrismaArgs,
        nested_creates: Vec<NestedCreateNode>,
        //nested_connects: Vec<Connect>,
    ) -> Self {
        let nested_mutactions = NestedMutactions {
            creates: nested_creates,
            ..Default::default()
        };

        Self {
            model,
            non_list_args,
            list_args,
            nested_mutactions,
        }
    }
}
