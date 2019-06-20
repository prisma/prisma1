use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct UpdateNode {
    pub where_: RecordFinder,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,
}

#[derive(Debug, Clone)]
pub struct NestedUpdateNode {
    pub relation_field: Arc<RelationField>,
    pub where_: Option<RecordFinder>,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,
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
    pub relation_field: Arc<RelationField>,
    pub filter: Option<Filter>,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
}
