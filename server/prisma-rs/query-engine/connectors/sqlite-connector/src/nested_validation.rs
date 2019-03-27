use connector::{ConnectorError, ConnectorResult, NestedCreateNode};
use prisma_models::*;
use prisma_query::ast::*;

pub trait NestedValidation {
    fn required_check(&self, parent_id: GraphqlId) -> ConnectorResult<Option<Query>>;
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

    fn check_for_old_child(&self, id: GraphqlId) -> Query {
        let relation = self.relation();
        let rf = self.relation_field();

        let relation_column = relation.column_for_relation_side(rf.relation_side);
        let opposite_column = relation.column_for_relation_side(rf.relation_side.opposite());

        Select::from(relation.relation_table())
            .column(opposite_column.clone())
            .so_that(opposite_column.equals(id).and(relation_column.is_not_null()))
            .into()
    }
}

impl NestedValidation for NestedCreateNode {
    fn relation_field(&self) -> RelationFieldRef {
        self.relation_field.clone()
    }

    fn relation(&self) -> RelationRef {
        self.relation_field().relation()
    }

    fn required_check(&self, parent_id: GraphqlId) -> ConnectorResult<Option<Query>> {
        if self.top_is_create {
            return Ok(None);
        }

        let p = self.relation_field.clone();
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
}
