use super::*;
use connector::{mutaction::NestedCreateNode, ConnectorResult};
use prisma_models::*;
use prisma_query::ast::*;
use std::sync::Arc;

impl NestedActions for NestedDeleteNode {
    fn relation_field(&self) -> RelationFieldRef {
        self.relation_field.clone()
    }

    fn relation(&self) -> RelationRef {
        self.relation_field().relation()
    }

    fn required_check(&self, _: &GraphqlId) -> ConnectorResult<Option<(Select, ResultCheck)>> {
        Ok(None)
    }

    fn parent_removal(&self, _: &GraphqlId) -> Option<Query> {
        None
    }

    fn child_removal(&self, _: &GraphqlId) -> Option<Query> {
        None
    }
}
