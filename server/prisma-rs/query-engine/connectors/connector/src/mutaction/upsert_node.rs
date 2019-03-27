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
    pub relation_field: Arc<RelationField>,
    pub where_: Option<NodeSelector>,
    pub create: NestedCreateNode,
    pub update: NestedUpdateNode,
}
