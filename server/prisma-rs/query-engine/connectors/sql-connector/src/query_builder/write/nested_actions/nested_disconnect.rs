use super::*;
use connector::write_ast::NestedDisconnect;
use prisma_models::*;
use prisma_query::ast::*;
use std::sync::Arc;

impl NestedActions for NestedDisconnect {
    fn relation_field(&self) -> RelationFieldRef {
        self.relation_field.clone()
    }

    fn relation(&self) -> RelationRef {
        self.relation_field().relation()
    }

    fn required_check(&self, _: &GraphqlId) -> crate::Result<Option<(Select<'static>, ResultCheck)>> {
        let p = Arc::clone(&self.relation_field);
        let c = p.related_field();

        match (p.is_list, p.is_required, c.is_list, c.is_required) {
            (false, true, false, true) => Err(self.relation_violation()),
            (false, true, false, false) => Err(self.relation_violation()),
            (false, false, false, true) => Err(self.relation_violation()),
            (false, false, false, false) => Ok(None),
            (true, false, false, true) => Err(self.relation_violation()),
            (true, false, false, false) => Ok(None),
            (false, true, true, false) => Err(self.relation_violation()),
            (false, false, true, false) => Ok(None),
            (true, false, true, false) => Ok(None),
            _ => unreachable!(),
        }
    }

    fn parent_removal(&self, _: &GraphqlId) -> Option<Query<'static>> {
        None
    }

    fn child_removal(&self, _: &GraphqlId) -> Option<Query<'static>> {
        None
    }
}
