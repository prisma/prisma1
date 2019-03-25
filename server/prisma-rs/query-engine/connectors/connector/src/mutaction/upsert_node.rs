use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct UpsertNode {
    pub where_: NodeSelector,
    pub create: CreateNode,
    pub update: UpdateNode,
}

#[derive(Debug, Clone)]
pub struct NestedUpsertNode {
    pub project: ProjectRef,
    pub where_: NodeSelector,
    pub create: NestedCreateNode,
    pub update: NestedUpdateNode,

    pub relation_field: Arc<RelationField>,
}
