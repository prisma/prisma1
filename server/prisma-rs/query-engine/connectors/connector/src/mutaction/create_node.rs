use prisma_models::prelude::*;
use super::NestedMutactions;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct CreateNode {
    pub project: ProjectRef,
    pub model: ModelRef,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,
}

#[derive(Debug, Clone)]
pub struct NestedCreateNode {
    pub project: ProjectRef,
    pub model: ModelRef,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,

    pub relation_field: Arc<RelationField>,
    pub top_is_create: bool,
}

impl CreateNode {
    pub fn new(
        project: ProjectRef,
        model: ModelRef,
        non_list_args: PrismaArgs,
        list_args: Vec<(String, PrismaListValue)>,
        nested_creates: Vec<NestedCreateNode>,
        //nested_connects: Vec<Connect>,
    ) -> Self {
        let nested_mutactions = NestedMutactions {
            creates: nested_creates,
            ..Default::default()
        };

        Self {
            project,
            model,
            non_list_args,
            list_args,
            nested_mutactions
        }
    }
}
