use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct DeleteNode {
    pub where_: NodeSelector,
}

#[derive(Debug, Clone)]
pub struct NestedDeleteNode {
    pub project: ProjectRef,
    pub where_: NodeSelector,

    pub relation_field: Arc<RelationField>,
}

#[derive(Debug, Clone)]
pub struct DeleteNodes {
    pub model: ModelRef,
    pub filter: Filter,
}

#[derive(Debug, Clone)]
pub struct NestedDeleteNodes {
    pub model: ModelRef,
    pub filter: Filter,

    pub relation_field: Arc<RelationField>,
}
