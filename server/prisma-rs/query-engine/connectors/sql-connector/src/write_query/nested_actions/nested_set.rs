use super::*;
use connector_interface::write_query::NestedSet;
use prisma_models::*;
use prisma_query::ast::*;

impl NestedActions for NestedSet {
    fn relation_field(&self) -> RelationFieldRef {
        self.relation_field.clone()
    }

    fn relation(&self) -> RelationRef {
        self.relation_field().relation()
    }

    fn required_check(&self, _: &GraphqlId) -> crate::Result<Option<(Select<'static>, ResultCheck)>> {
        Ok(None)
    }

    fn parent_removal(&self, _: &GraphqlId) -> Option<Query<'static>> {
        None
    }

    fn child_removal(&self, _: &GraphqlId) -> Option<Query<'static>> {
        None
    }
}
