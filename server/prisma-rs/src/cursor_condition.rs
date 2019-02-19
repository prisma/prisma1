use crate::{models::prelude::*, protobuf::prelude::*};
use sql::{grammar::clause::ConditionTree, prelude::*};

#[derive(Clone, Copy)]
enum CursorType {
    Before,
    After,
}

pub struct CursorCondition;

impl CursorCondition {
    pub fn build(query_arguments: &QueryArguments, model: &Model) -> ConditionTree {
        match (
            query_arguments.before.as_ref(),
            query_arguments.after.as_ref(),
            query_arguments.order_by.as_ref(),
        ) {
            (None, None, _) => ConditionTree::NoCondition,
            (before, after, order_by) => {
                let field = match order_by {
                    Some(order) => model
                        .fields()
                        .find_from_scalar(&order.scalar_field)
                        .unwrap(),
                    None => model.fields().id(),
                };

                let sort_order: Order = order_by
                    .map(|order| order.sort_order().into())
                    .unwrap_or(Order::Ascending);

                let cursor_for = |cursor_type: CursorType, id: IdValue| {
                    let row = Row::from((field.model_column(), model.fields().id().model_column()));

                    let where_condition = model.fields().id().model_column().equals(id.clone());

                    let select_query = select_from(&model.table())
                        .column(field.model_column())
                        .column(id.clone())
                        .so_that(where_condition);

                    let compare = match (cursor_type, sort_order) {
                        (CursorType::Before, Order::Ascending) => row.less_than(select_query),
                        (CursorType::Before, Order::Descending) => row.greater_than(select_query),
                        (CursorType::After, Order::Ascending) => row.greater_than(select_query),
                        (CursorType::After, Order::Descending) => row.less_than(select_query),
                    };

                    ConditionTree::single(compare)
                };

                let after_cursor = after
                    .map(|id| {
                        let id_val = id.id_value.clone().unwrap();
                        cursor_for(CursorType::After, id_val)
                    })
                    .unwrap_or(ConditionTree::NoCondition);
                let before_cursor = before
                    .map(|id| {
                        let id_val = id.id_value.clone().unwrap();
                        cursor_for(CursorType::Before, id_val)
                    })
                    .unwrap_or(ConditionTree::NoCondition);

                ConditionTree::and(after_cursor, before_cursor)
            }
        }
    }
}
