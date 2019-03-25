use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct UpdateNode {
    pub where_: NodeSelector,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,
}

#[derive(Debug, Clone)]
pub struct NestedUpdateNode {
    pub project: ProjectRef,
    pub where_: NodeSelector,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,

    pub relation_field: Arc<RelationField>,
}

#[derive(Debug, Clone)]
pub struct UpdateNodes {
    pub model: ModelRef,
    pub filter: Filter,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
}

#[derive(Debug, Clone)]
pub struct NestedUpdateNodes {
    pub model: ModelRef,
    pub filter: Filter,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub relation_field: Arc<RelationField>,
}
