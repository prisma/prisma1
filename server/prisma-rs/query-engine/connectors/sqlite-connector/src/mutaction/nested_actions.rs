use crate::query_builder::QueryBuilder;
use connector::{
    error::*,
    filter::NodeSelector,
    mutaction::{NestedConnect, NestedCreateNode},
    ConnectorResult,
};
use prisma_models::*;
use prisma_query::ast::*;
use std::sync::Arc;

// TODO: Replace me with FnBox from std when it's stabilized in 1.35.
// https://doc.rust-lang.org/std/boxed/trait.FnBox.html
pub trait FnBox {
    fn call_box(self: Box<Self>, o: Option<GraphqlId>) -> ConnectorResult<()>;
}

// TODO: Replace me with FnBox from std when it's stabilized in 1.35.
// https://doc.rust-lang.org/std/boxed/trait.FnBox.html
impl<F> FnBox for F
where
    F: FnOnce(Option<GraphqlId>) -> ConnectorResult<()>,
{
    fn call_box(self: Box<F>, o: Option<GraphqlId>) -> ConnectorResult<()> {
        (*self)(o)
    }
}

pub type ResultCheck = Box<FnBox + Send + Sync + 'static>;

pub trait NestedActions {
    fn required_check(&self, parent_id: &GraphqlId) -> ConnectorResult<Option<(Query, ResultCheck)>>;

    fn parent_removal(&self, parent_id: &GraphqlId) -> Option<Query>;
    fn child_removal(&self, child_id: &GraphqlId) -> Option<Query>;

    fn relation_field(&self) -> RelationFieldRef;
    fn relation(&self) -> RelationRef;

    fn relation_violation(&self) -> ConnectorError {
        let relation = self.relation();

        ConnectorError::RelationViolation {
            relation_name: relation.name.clone(),
            model_a_name: relation.model_a().name.clone(),
            model_b_name: relation.model_b().name.clone(),
        }
    }

    fn removal_by_parent(&self, id: &GraphqlId) -> Query {
        let rf = self.relation_field();
        let relation = self.relation();
        let relation_column = relation.column_for_relation_side(rf.relation_side);

        let condition = relation_column.equals(id.clone());

        match relation.inline_relation_column() {
            Some(column) => Update::table(relation.relation_table())
                .set(column.name.as_ref(), PrismaValue::Null)
                .so_that(condition)
                .into(),
            None => Delete::from_table(relation.relation_table()).so_that(condition).into(),
        }
    }

    fn removal_by_child(&self, id: &GraphqlId) -> Query {
        let rf = self.relation_field();
        assert!(!rf.related_field().is_list);

        let relation = self.relation();

        let condition = relation
            .column_for_relation_side(rf.relation_side.opposite())
            .equals(id.clone());

        match relation.inline_relation_column() {
            Some(column) => Update::table(relation.relation_table())
                .set(column.name.as_ref(), PrismaValue::Null)
                .so_that(condition)
                .into(),
            None => Delete::from_table(relation.relation_table()).so_that(condition).into(),
        }
    }

    fn check_for_old_child(&self, id: &GraphqlId) -> (Query, ResultCheck) {
        let relation = self.relation();
        let rf = self.relation_field().related_field();

        let relation_column = relation.column_for_relation_side(rf.relation_side);
        let opposite_column = relation.column_for_relation_side(rf.relation_side.opposite());

        let query = Select::from_table(relation.relation_table())
            .column(opposite_column.clone())
            .so_that(opposite_column.equals(id.clone()).and(relation_column.is_not_null()))
            .into();

        let error = self.relation_violation();

        let check = |row_opt: Option<GraphqlId>| {
            if row_opt.is_some() {
                Err(error)
            } else {
                Ok(())
            }
        };

        (query, Box::new(check))
    }

    fn check_for_old_parent_by_child(&self, node_selector: &NodeSelector) -> (Query, ResultCheck) {
        let relation = self.relation();
        let rf = self.relation_field().related_field();

        let relation_column = relation.column_for_relation_side(rf.relation_side);
        let opposite_column = relation.column_for_relation_side(rf.relation_side.opposite());

        let sub_select = QueryBuilder::get_nodes(
            rf.model(),
            &SelectedFields::from(rf.model().fields().id()),
            node_selector.clone(),
        );

        let condition = relation_column
            .clone()
            .in_selection(sub_select)
            .and(opposite_column.clone().is_not_null());

        let query = Select::from_table(relation.relation_table())
            .column(opposite_column)
            .so_that(condition)
            .into();

        let error = self.relation_violation();

        let check = |row_opt: Option<GraphqlId>| {
            if row_opt.is_some() {
                Err(error)
            } else {
                Ok(())
            }
        };

        (query, Box::new(check))
    }
}

impl NestedActions for NestedConnect {
    fn relation_field(&self) -> RelationFieldRef {
        self.relation_field.clone()
    }

    fn relation(&self) -> RelationRef {
        self.relation_field().relation()
    }

    fn required_check(&self, parent_id: &GraphqlId) -> ConnectorResult<Option<(Query, ResultCheck)>> {
        let p = Arc::clone(&self.relation_field);
        let c = p.related_field();

        if self.top_is_create {
            match (p.is_list, p.is_required, c.is_list, c.is_required) {
                (false, true, false, true) => Err(self.relation_violation()),
                (false, true, false, false) => Ok(Some(self.check_for_old_parent_by_child(&self.where_))),
                (false, false, false, true) => Ok(None),
                (false, false, false, false) => Ok(None),
                (true, false, false, true) => Ok(None),
                (true, false, false, false) => Ok(None),
                (false, true, true, false) => Ok(None),
                (false, false, true, false) => Ok(None),
                (true, false, true, false) => Ok(None),
                _ => unreachable!(),
            }
        } else {
            match (p.is_list, p.is_required, c.is_list, c.is_required) {
                (false, true, false, true) => Err(self.relation_violation()),
                (false, true, false, false) => Ok(Some(self.check_for_old_parent_by_child(&self.where_))),
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
    }

    fn parent_removal(&self, parent_id: &GraphqlId) -> Option<Query> {
        let p = self.relation_field.clone();
        let c = p.related_field();

        if self.top_is_create {
            match (p.is_list, c.is_list) {
                (false, false) => Some(self.removal_by_parent(parent_id)),
                (true, false) => None,
                (false, true) => None,
                (true, true) => None,
            }
        } else {
            match (p.is_list, c.is_list) {
                (false, false) => Some(self.removal_by_parent(parent_id)),
                (true, false) => None,
                (false, true) => Some(self.removal_by_parent(parent_id)),
                (true, true) => None,
            }
        }
    }

    fn child_removal(&self, child_id: &GraphqlId) -> Option<Query> {
        let p = self.relation_field.clone();
        let c = p.related_field();

        if self.top_is_create {
            match (p.is_list, c.is_list) {
                (false, false) => None,
                (true, false) => Some(self.removal_by_child(child_id)),
                (false, true) => None,
                (true, true) => None,
            }
        } else {
            match (p.is_list, c.is_list) {
                (false, false) => Some(self.removal_by_child(child_id)),
                (true, false) => Some(self.removal_by_child(child_id)),
                (false, true) => None,
                (true, true) => None,
            }
        }
    }
}

impl NestedActions for NestedCreateNode {
    fn relation_field(&self) -> RelationFieldRef {
        self.relation_field.clone()
    }

    fn relation(&self) -> RelationRef {
        self.relation_field().relation()
    }

    fn required_check(&self, parent_id: &GraphqlId) -> ConnectorResult<Option<(Query, ResultCheck)>> {
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

    fn parent_removal(&self, parent_id: &GraphqlId) -> Option<Query> {
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

    fn child_removal(&self, _: &GraphqlId) -> Option<Query> {
        None
    }
}
