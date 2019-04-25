mod nested_connect;
mod nested_create_node;
mod nested_delete_node;
mod nested_disconnect;
mod nested_set;

pub use nested_connect::*;
pub use nested_create_node::*;
pub use nested_delete_node::*;
pub use nested_disconnect::*;
pub use nested_set::*;

use crate::query_builder::QueryBuilder;
use connector::{error::*, filter::NodeSelector, ConnectorResult};
use prisma_models::*;
use prisma_query::ast::*;

// TODO: Replace me with FnBox from std when it's stabilized in 1.35.
// https://doc.rust-lang.org/std/boxed/trait.FnBox.html
pub trait FnBox {
    fn call_box(self: Box<Self>, exists: bool) -> ConnectorResult<()>;
}

// TODO: Replace me with FnBox from std when it's stabilized in 1.35.
// https://doc.rust-lang.org/std/boxed/trait.FnBox.html
impl<F> FnBox for F
where
    F: FnOnce(bool) -> ConnectorResult<()>,
{
    fn call_box(self: Box<F>, exists: bool) -> ConnectorResult<()> {
        (*self)(exists)
    }
}

pub type ResultCheck = Box<FnBox + Send + Sync + 'static>;

pub trait NestedActions {
    fn required_check(&self, parent_id: &GraphqlId) -> ConnectorResult<Option<(Select, ResultCheck)>>;

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

    fn nodes_not_connected(&self, parent_id: Option<GraphqlId>, child_id: Option<GraphqlId>) -> ConnectorError {
        let rf = self.relation_field();

        let parent_where = parent_id.map(|parent_id| NodeSelectorInfo::for_id(rf.model(), &parent_id));
        let child_where = child_id.map(|child_id| NodeSelectorInfo::for_id(rf.model(), &child_id));

        ConnectorError::NodesNotConnected {
            relation_name: rf.relation().name.clone(),
            parent_name: rf.model().name.clone(),
            parent_where,
            child_name: rf.related_model().name.clone(),
            child_where,
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

    fn removal_by_parent_and_child(&self, parent_id: &GraphqlId, child_id: &GraphqlId) -> Query {
        let relation = self.relation();
        let rf = self.relation_field();

        let is_child = rf.opposite_column().equals(child_id.clone());
        let is_parent = rf.relation_column().equals(parent_id.clone());

        let table = relation.relation_table();

        match relation.inline_relation_column() {
            Some(column) => Update::table(table)
                .set(column.name.as_ref(), PrismaValue::Null)
                .so_that(is_child.and(is_parent))
                .into(),
            None => Delete::from_table(table).so_that(is_child.and(is_parent)).into(),
        }
    }

    fn check_for_old_child(&self, id: &GraphqlId) -> (Select, ResultCheck) {
        let relation = self.relation();
        let rf = self.relation_field().related_field();

        let relation_column = relation.column_for_relation_side(rf.relation_side);
        let opposite_column = relation.column_for_relation_side(rf.relation_side.opposite());

        let query = Select::from_table(relation.relation_table())
            .column(opposite_column.clone())
            .so_that(opposite_column.equals(id.clone()).and(relation_column.is_not_null()));

        let error = self.relation_violation();

        let check = |exists: bool| {
            if exists {
                Err(error)
            } else {
                Ok(())
            }
        };

        (query, Box::new(check))
    }

    fn check_for_old_parent_by_child(&self, node_selector: &NodeSelector) -> (Select, ResultCheck) {
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
            .so_that(condition);

        let error = self.relation_violation();

        let check = |exists: bool| {
            if exists {
                Err(error)
            } else {
                Ok(())
            }
        };

        (query, Box::new(check))
    }

    fn ensure_parent_is_connected(&self, parent_id: &GraphqlId) -> (Select, ResultCheck) {
        let relation = self.relation();
        let rf = self.relation_field();

        let is_parent = rf.relation_column().equals(parent_id);
        let child_exists = rf.opposite_column().is_not_null();

        let query = Select::from_table(relation.relation_table())
            .column(rf.relation_column())
            .so_that(is_parent.and(child_exists));

        let error = self.nodes_not_connected(Some(parent_id.clone()), None);

        let check = |exists: bool| {
            if exists {
                Ok(())
            } else {
                Err(error)
            }
        };

        (query, Box::new(check))
    }

    fn ensure_connected(&self, child_id: &GraphqlId, parent_id: &GraphqlId) -> (Select, ResultCheck) {
        let relation = self.relation();
        let rf = self.relation_field();

        let condition = rf
            .opposite_column()
            .equals(parent_id)
            .and(rf.relation_column().equals(child_id));

        let query = Select::from_table(relation.relation_table())
            .column(rf.opposite_column())
            .so_that(condition);

        let error = self.nodes_not_connected(Some(parent_id.clone()), Some(child_id.clone()));

        let check = |exists: bool| {
            if exists {
                Ok(())
            } else {
                Err(error)
            }
        };

        (query, Box::new(check))
    }
}
