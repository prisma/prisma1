use crate::filter::NodeSelector;
use prisma_models::RelationFieldRef;

#[derive(Debug, Clone)]
pub struct NestedConnect {
    pub relation_field: RelationFieldRef,
    pub where_: NodeSelector,
    pub top_is_create: bool,
}
