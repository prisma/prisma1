use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

pub trait SharedUpdateLogic {
    fn model(&self) -> ModelRef;
    fn non_list_args(&self) -> &PrismaArgs;
    fn list_args(&self) -> &[(String, PrismaListValue)];
    fn nested_mutactions(&self) -> &NestedMutactions;
}

#[derive(Debug, Clone)]
pub struct UpdateNode {
    pub where_: NodeSelector,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,
}

impl SharedUpdateLogic for UpdateNode {
    fn model(&self) -> ModelRef {
        self.where_.field.model()
    }

    fn non_list_args(&self) -> &PrismaArgs {
        &self.non_list_args
    }

    fn list_args(&self) -> &[(String, PrismaListValue)] {
        self.list_args.as_slice()
    }

    fn nested_mutactions(&self) -> &NestedMutactions {
        &self.nested_mutactions
    }
}

#[derive(Debug, Clone)]
pub struct NestedUpdateNode {
    pub relation_field: Arc<RelationField>,
    pub where_: Option<NodeSelector>,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_mutactions: NestedMutactions,
}

impl SharedUpdateLogic for NestedUpdateNode {
    fn model(&self) -> ModelRef {
        self.relation_field.related_model()
    }

    fn non_list_args(&self) -> &PrismaArgs {
        &self.non_list_args
    }

    fn list_args(&self) -> &[(String, PrismaListValue)] {
        self.list_args.as_slice()
    }

    fn nested_mutactions(&self) -> &NestedMutactions {
        &self.nested_mutactions
    }
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
