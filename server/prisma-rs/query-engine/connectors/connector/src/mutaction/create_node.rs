use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct CreateNode {
    pub model: ModelRef,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,
}

#[derive(Debug, Clone)]
pub struct NestedCreateNode {
    pub relation_field: Arc<RelationField>,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub top_is_create: bool,
    pub nested_mutactions: NestedMutactions,
}

impl CreateNode {
    pub fn new(
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
            model,
            non_list_args,
            list_args,
            nested_mutactions,
        }
    }
}

impl From<CreateNode> for DatabaseMutaction {
    fn from(cn: CreateNode) -> DatabaseMutaction {
        DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::CreateNode(cn))
    }
}
