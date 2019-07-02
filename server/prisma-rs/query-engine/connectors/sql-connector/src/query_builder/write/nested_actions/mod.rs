mod nested_connect;
mod nested_create_record;
mod nested_delete_record;
mod nested_disconnect;
mod nested_set;

pub use nested_connect::*;
pub use nested_create_record::*;
pub use nested_delete_record::*;
pub use nested_disconnect::*;
pub use nested_set::*;

use crate::{error::*, query_builder::read::ReadQueryBuilder, SqlResult};
use connector::{error::RecordFinderInfo, filter::RecordFinder};
use prisma_models::*;
use prisma_query::ast::*;

pub type ResultCheck = Box<FnOnce(bool) -> SqlResult<()> + Send + Sync + 'static>;

pub trait NestedActions {
    fn required_check(&self, parent_id: &GraphqlId) -> SqlResult<Option<(Select<'static>, ResultCheck)>>;

    fn parent_removal(&self, parent_id: &GraphqlId) -> Option<Query<'static>>;
    fn child_removal(&self, child_id: &GraphqlId) -> Option<Query<'static>>;

    fn relation_field(&self) -> RelationFieldRef;
    fn relation(&self) -> RelationRef;

    fn relation_violation(&self) -> SqlError {
        let relation = self.relation();

        SqlError::RelationViolation {
            relation_name: relation.name.clone(),
            model_a_name: relation.model_a().name.clone(),
            model_b_name: relation.model_b().name.clone(),
        }
    }

    fn records_not_connected(&self, parent_id: Option<GraphqlId>, child_id: Option<GraphqlId>) -> SqlError {
        let rf = self.relation_field();

        let parent_where = parent_id.map(|parent_id| RecordFinderInfo::for_id(rf.model(), &parent_id));
        let child_where = child_id.map(|child_id| RecordFinderInfo::for_id(rf.model(), &child_id));

        SqlError::RecordsNotConnected {
            relation_name: rf.relation().name.clone(),
            parent_name: rf.model().name.clone(),
            parent_where,
            child_name: rf.related_model().name.clone(),
            child_where,
        }
    }

    fn removal_by_parent(&self, id: &GraphqlId) -> Query<'static> {
        let rf = self.relation_field();
        let relation = self.relation();
        let relation_column = relation.column_for_relation_side(rf.relation_side);

        let condition = relation_column.equals(id.clone());

        match relation.inline_relation_column() {
            Some(column) => Update::table(relation.relation_table())
                .set(column.name.to_string(), PrismaValue::Null)
                .so_that(condition)
                .into(),
            None => Delete::from_table(relation.relation_table()).so_that(condition).into(),
        }
    }

    fn removal_by_child(&self, id: &GraphqlId) -> Query<'static> {
        let rf = self.relation_field();
        assert!(!rf.related_field().is_list);

        let relation = self.relation();

        let condition = relation
            .column_for_relation_side(rf.relation_side.opposite())
            .equals(id.clone());

        match relation.inline_relation_column() {
            Some(column) => Update::table(relation.relation_table())
                .set(column.name.to_string(), PrismaValue::Null)
                .so_that(condition)
                .into(),
            None => Delete::from_table(relation.relation_table()).so_that(condition).into(),
        }
    }

    fn removal_by_parent_and_child(&self, parent_id: &GraphqlId, child_id: &GraphqlId) -> Query<'static> {
        let relation = self.relation();
        let rf = self.relation_field();

        let is_child = rf.opposite_column().equals(child_id.clone());
        let is_parent = rf.relation_column().equals(parent_id.clone());

        let table = relation.relation_table();

        match relation.inline_relation_column() {
            Some(column) => Update::table(table)
                .set(column.name.to_string(), PrismaValue::Null)
                .so_that(is_child.and(is_parent))
                .into(),
            None => Delete::from_table(table).so_that(is_child.and(is_parent)).into(),
        }
    }

    fn check_for_old_child(&self, id: &GraphqlId) -> (Select<'static>, ResultCheck) {
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

    fn check_for_old_parent_by_child(&self, record_finder: &RecordFinder) -> (Select<'static>, ResultCheck) {
        let relation = self.relation();
        let rf = self.relation_field().related_field();

        let relation_column = relation.column_for_relation_side(rf.relation_side);
        let opposite_column = relation.column_for_relation_side(rf.relation_side.opposite());

        let sub_select = ReadQueryBuilder::get_records(
            rf.model(),
            &SelectedFields::from(rf.model().fields().id()),
            record_finder.clone(),
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

    fn ensure_parent_is_connected(&self, parent_id: &GraphqlId) -> (Select<'static>, ResultCheck) {
        let relation = self.relation();
        let rf = self.relation_field();

        let is_parent = rf.relation_column().equals(parent_id);
        let child_exists = rf.opposite_column().is_not_null();

        let query = Select::from_table(relation.relation_table())
            .column(rf.relation_column())
            .so_that(is_parent.and(child_exists));

        let error = self.records_not_connected(Some(parent_id.clone()), None);

        let check = |exists: bool| {
            if exists {
                Ok(())
            } else {
                Err(error)
            }
        };

        (query, Box::new(check))
    }

    fn ensure_connected(&self, child_id: &GraphqlId, parent_id: &GraphqlId) -> (Select<'static>, ResultCheck) {
        let relation = self.relation();
        let rf = self.relation_field();

        let condition = rf
            .opposite_column()
            .equals(parent_id)
            .and(rf.relation_column().equals(child_id));

        let query = Select::from_table(relation.relation_table())
            .column(rf.opposite_column())
            .so_that(condition);

        let error = self.records_not_connected(Some(parent_id.clone()), Some(child_id.clone()));

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
