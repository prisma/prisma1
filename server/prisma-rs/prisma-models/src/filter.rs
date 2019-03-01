use crate::{PrismaValue, RelationField, ScalarField};
use prisma_query::ast::*;
use std::sync::Arc;

pub enum Filter {
    And(Vec<Box<Filter>>),
    Or(Vec<Box<Filter>>),
    Not(Vec<Box<Filter>>),
    Scalar(ScalarFilter),
    ScalarList(ScalarListFilter),
    OneRelationIsNull(OneRelationIsNullFilter),
    Relation(RelationFilter),
    NodeSubscription,
    BoolFilter(bool),
}

pub enum ScalarCondition {
    Equals(PrismaValue),
    NotEquals(PrismaValue),
    Contains(PrismaValue),
    NotContains(PrismaValue),
    StartsWith(PrismaValue),
    NotStartsWith(PrismaValue),
    EndsWith(PrismaValue),
    NotEndsWith(PrismaValue),
    LessThan(PrismaValue),
    LessThanOrEquals(PrismaValue),
    GreaterThan(PrismaValue),
    GreaterThanOrEquals(PrismaValue),
    In(Vec<PrismaValue>),
    NotIn(Vec<PrismaValue>),
}

pub enum ScalarListCondition {
    Contains(PrismaValue),
    ContainsEvery(Vec<PrismaValue>),
    ContainsSome(Vec<PrismaValue>),
}

pub struct OneRelationIsNullFilter {
    pub field: Arc<RelationField>,
}

pub struct ScalarListFilter {
    pub field: Arc<ScalarField>,
    pub condition: ScalarListCondition,
}

pub struct ScalarFilter {
    pub field: Arc<ScalarField>,
    pub condition: ScalarCondition,
}

pub struct RelationFilter {
    pub field: Arc<RelationField>,
    pub nested_filter: Box<Filter>,
    pub condition: RelationCondition,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub enum RelationCondition {
    EveryRelatedNode,
    AtLeastOneRelatedNode,
    NoRelatedNode,
    ToOneRelatedNode,
}

impl RelationCondition {
    pub fn invert_of_subselect(&self) -> bool {
        match self {
            RelationCondition::EveryRelatedNode => true,
            _ => false,
        }
    }
}

impl From<Filter> for ConditionTree {
    fn from(filter: Filter) -> ConditionTree {
        match filter {
            Filter::And(mut filters) => match filters.pop() {
                None => ConditionTree::NoCondition,
                Some(filter) => {
                    let right = ConditionTree::from(*filter);

                    filters.into_iter().rev().fold(right, |acc, filter| {
                        let left = ConditionTree::from(*filter);
                        ConditionTree::and(left, acc)
                    })
                }
            },
            Filter::Or(mut filters) => match filters.pop() {
                None => ConditionTree::NoCondition,
                Some(filter) => {
                    let right = ConditionTree::from(*filter);

                    filters.into_iter().rev().fold(right, |acc, filter| {
                        let left = ConditionTree::from(*filter);
                        ConditionTree::or(left, acc)
                    })
                }
            },
            Filter::Not(filters) => ConditionTree::not(ConditionTree::from(Filter::And(filters))),
            Filter::Scalar(filter) => ConditionTree::from(filter),
            Filter::OneRelationIsNull(filter) => ConditionTree::from(filter),
            Filter::Relation(filter) => filter.into(),
            Filter::BoolFilter(b) => {
                if b {
                    ConditionTree::NoCondition
                } else {
                    ConditionTree::NegativeCondition
                }
            }
            _ => unimplemented!(),
        }
    }
}

impl From<ScalarFilter> for ConditionTree {
    fn from(filter: ScalarFilter) -> ConditionTree {
        let column = filter.field.as_column();

        let condition = match filter.condition {
            ScalarCondition::Equals(PrismaValue::Null) => column.is_null(),
            ScalarCondition::NotEquals(PrismaValue::Null) => column.is_not_null(),
            ScalarCondition::Equals(value) => column.equals(value),
            ScalarCondition::NotEquals(value) => column.not_equals(value),
            ScalarCondition::Contains(value) => column.like(format!("{}", value)),
            ScalarCondition::NotContains(value) => column.not_like(format!("{}", value)),
            ScalarCondition::StartsWith(value) => column.begins_with(format!("{}", value)),
            ScalarCondition::NotStartsWith(value) => column.not_begins_with(format!("{}", value)),
            ScalarCondition::EndsWith(value) => column.ends_into(format!("{}", value)),
            ScalarCondition::NotEndsWith(value) => column.not_ends_into(format!("{}", value)),
            ScalarCondition::LessThan(value) => column.less_than(value),
            ScalarCondition::LessThanOrEquals(value) => column.less_than_or_equals(value),
            ScalarCondition::GreaterThan(value) => column.greater_than(value),
            ScalarCondition::GreaterThanOrEquals(value) => column.greater_than_or_equals(value),
            ScalarCondition::In(values) => match values.split_first() {
                Some((PrismaValue::Null, tail)) if tail.is_empty() => column.is_null(),
                _ => column.in_selection(values),
            },
            ScalarCondition::NotIn(values) => match values.split_first() {
                Some((PrismaValue::Null, tail)) if tail.is_empty() => column.is_not_null(),
                _ => column.not_in_selection(values),
            },
        };

        ConditionTree::single(condition)
    }
}

impl RelationFilter {
    pub fn conditions(
        column: Column,
        condition: RelationCondition,
        sub_select: Select,
    ) -> ConditionTree {
        match condition {
            RelationCondition::EveryRelatedNode => column.not_in_selection(sub_select),
            RelationCondition::AtLeastOneRelatedNode => column.not_in_selection(sub_select),
            RelationCondition::NoRelatedNode => column.in_selection(sub_select),
            RelationCondition::ToOneRelatedNode => column.in_selection(sub_select),
        }
        .into()
    }
}

impl From<RelationFilter> for ConditionTree {
    fn from(filter: RelationFilter) -> ConditionTree {
        RelationFilter::conditions(
            filter.field.model().id_column(),
            filter.condition.clone(),
            filter.into(),
        )
    }
}

impl From<RelationFilter> for Select {
    fn from(filter: RelationFilter) -> Select {
        let condition = filter.condition.clone();
        let relation = filter.field.relation();
        let this_column = relation.column_for_relation_side(filter.field.relation_side);
        let other_column = relation.column_for_relation_side(filter.field.relation_side.opposite());
        let nested_filter = *filter.nested_filter;

        match nested_filter {
            Filter::Relation(filter) => {
                let sub_select: Select = filter.into();
                let tree = RelationFilter::conditions(other_column, condition, sub_select.into());
                let conditions = tree.invert_if(condition.invert_of_subselect());

                Select::from(relation.relation_table())
                    .column(this_column)
                    .so_that(conditions)
            }
            _ => {
                let tree = ConditionTree::from(nested_filter);
                let id_column = filter.field.related_model().id_column();

                let join = filter
                    .field
                    .related_model()
                    .table()
                    .on(id_column.clone().equals(other_column));

                Select::from(relation.relation_table())
                    .column(this_column)
                    .inner_join(join)
                    .so_that(tree.invert_if(condition.invert_of_subselect()))
            }
        }
    }
}

impl From<OneRelationIsNullFilter> for ConditionTree {
    fn from(_filter: OneRelationIsNullFilter) -> ConditionTree {
        unimplemented!()
    }
}
