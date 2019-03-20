use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct CreateNode {
    pub project: ProjectRef,
    pub model: ModelRef,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,
    pub include_relay_row: bool,
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
        include_relay_row: bool,
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
            nested_mutactions,
            include_relay_row,
        }
    }
}

impl From<CreateNode> for DatabaseMutaction {
    fn from(cn: CreateNode) -> DatabaseMutaction {
        DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::CreateNode(cn))
    }
}
