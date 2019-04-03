use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct DeleteNode {
    pub where_: NodeSelector,
}

#[derive(Debug, Clone)]
pub struct NestedDeleteNode {
    pub relation_field: Arc<RelationField>,
    pub where_: Option<NodeSelector>,
}

#[derive(Debug, Clone)]
pub struct DeleteNodes {
    pub model: ModelRef,
    pub filter: Filter,
}

#[derive(Debug, Clone)]
pub struct NestedDeleteNodes {
    pub relation_field: Arc<RelationField>,
    pub filter: Option<Filter>,

}
