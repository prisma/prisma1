use connector::{Filter, OneRelationIsNullFilter, RelationCondition, RelationFilter, ScalarCondition, ScalarFilter};
use prisma_models::prelude::*;
use prisma_query::ast::*;

pub fn filter_to_condition_tree(filter: Filter) -> ConditionTree {
    match filter {
        Filter::And(mut filters) => match filters.pop() {
            None => ConditionTree::NoCondition,
            Some(filter) => {
                let right = filter_to_condition_tree(*filter);

                filters.into_iter().rev().fold(right, |acc, filter| {
                    let left = filter_to_condition_tree(*filter);
                    ConditionTree::and(left, acc)
                })
            }
        },
        Filter::Or(mut filters) => match filters.pop() {
            None => ConditionTree::NoCondition,
            Some(filter) => {
                let right = filter_to_condition_tree(*filter);

                filters.into_iter().rev().fold(right, |acc, filter| {
                    let left = filter_to_condition_tree(*filter);
                    ConditionTree::or(left, acc)
                })
            }
        },
        Filter::Not(mut filters) => match filters.pop() {
            None => ConditionTree::NoCondition,
            Some(filter) => {
                let right = filter_to_condition_tree(*filter).not();

                filters.into_iter().rev().fold(right, |acc, filter| {
                    let left = filter_to_condition_tree(*filter).not();
                    ConditionTree::and(left, acc)
                })
            }
        },
        Filter::Scalar(filter) => scalar_filter_to_condition_tree(filter),
        Filter::OneRelationIsNull(filter) => one_relation_isnull_filter_to_condition_tree(filter),
        Filter::Relation(filter) => relation_filter_to_condition_tree(filter),
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

fn scalar_filter_to_condition_tree(filter: ScalarFilter) -> ConditionTree {
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

fn relation_filter_to_condition_tree(filter: RelationFilter) -> ConditionTree {
    relation_filter_conditions(
        filter.field.model().id_column(),
        filter.condition.clone(),
        relation_filter_to_select(filter),
    )
}

fn relation_filter_to_select(filter: RelationFilter) -> Select {
    let condition = filter.condition.clone();
    let relation = filter.field.relation();
    let this_column = relation.column_for_relation_side(filter.field.relation_side);
    let other_column = relation.column_for_relation_side(filter.field.relation_side.opposite());
    let nested_filter = *filter.nested_filter;

    match nested_filter {
        Filter::Relation(filter) => {
            let sub_select: Select = relation_filter_to_select(filter);
            let tree = relation_filter_conditions(other_column, condition, sub_select);
            let conditions = tree.invert_if(condition.invert_of_subselect());

            Select::from(relation.relation_table())
                .column(this_column)
                .so_that(conditions)
        }
        _ => {
            let tree = filter_to_condition_tree(nested_filter);
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

fn relation_filter_conditions(column: Column, condition: RelationCondition, sub_select: Select) -> ConditionTree {
    match condition {
        RelationCondition::EveryRelatedNode => column.not_in_selection(sub_select),
        RelationCondition::NoRelatedNode => column.not_in_selection(sub_select),
        RelationCondition::AtLeastOneRelatedNode => column.in_selection(sub_select),
        RelationCondition::ToOneRelatedNode => column.in_selection(sub_select),
    }
    .into()
}

fn one_relation_isnull_filter_to_condition_tree(filter: OneRelationIsNullFilter) -> ConditionTree {
    let condition = if filter.field.relation_is_inlined_in_parent() {
        filter.field.as_column().is_null()
    } else {
        let relation = filter.field.relation();
        let column = relation.column_for_relation_side(filter.field.relation_side);
        let select = Select::from(relation.relation_table()).column(column);

        filter.field.model().id_column().not_in_selection(select)
    };

    ConditionTree::single(condition)
}
