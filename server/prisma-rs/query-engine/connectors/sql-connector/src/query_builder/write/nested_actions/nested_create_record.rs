use super::*;
use crate::SqlResult;
use connector::write_ast::NestedCreateRecord;
use prisma_models::*;
use prisma_query::ast::*;
use std::sync::Arc;

impl NestedActions for NestedCreateRecord {
    fn relation_field(&self) -> RelationFieldRef {
        self.relation_field.clone()
    }

    fn relation(&self) -> RelationRef {
        self.relation_field().relation()
    }

    fn required_check(&self, parent_id: &GraphqlId) -> SqlResult<Option<(Select<'static>, ResultCheck)>> {
        if self.top_is_create {
            return Ok(None);
        }

        let p = Arc::clone(&self.relation_field);
        let c = p.related_field();

        match (p.is_list, p.is_required, c.is_list, c.is_required) {
            (false, true, false, true) => Err(self.relation_violation()),
            (false, true, false, false) => Ok(None),
            (false, false, false, true) => Ok(Some(self.check_for_old_child(parent_id))),
            (false, false, false, false) => Ok(None),
            (true, false, false, true) => Ok(None),
            (true, false, false, false) => Ok(None),
            (false, true, true, false) => Ok(None),
            (false, false, true, false) => Ok(None),
            (true, false, true, false) => Ok(None),
            _ => unreachable!(),
        }
    }

    fn parent_removal(&self, parent_id: &GraphqlId) -> Option<Query<'static>> {
        if self.top_is_create {
            return None;
        }

        let p = self.relation_field.clone();
        let c = p.related_field();

        match (p.is_list, c.is_list) {
            (false, false) => Some(self.removal_by_parent(parent_id)),
            (true, false) => None,
            (false, true) => Some(self.removal_by_parent(parent_id)),
            (true, true) => None,
        }
    }

    fn child_removal(&self, _: &GraphqlId) -> Option<Query<'static>> {
        None
    }
}
