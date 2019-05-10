use connector::QueryArguments;
use prisma_models::prelude::*;
use prisma_query::ast::*;
use std::sync::Arc;

#[derive(Clone, Copy)]
enum CursorType {
    Before,
    After,
}

pub struct CursorCondition;

impl CursorCondition {
    pub fn build(query_arguments: &QueryArguments, model: ModelRef) -> ConditionTree {
        match (
            query_arguments.before.as_ref(),
            query_arguments.after.as_ref(),
            query_arguments.order_by.as_ref(),
        ) {
            (None, None, _) => ConditionTree::NoCondition,
            (before, after, order_by) => {
                let field = match order_by {
                    Some(order) => Arc::clone(&order.field),
                    None => model.fields().id(),
                };

                let sort_order: SortOrder = order_by.map(|order| order.sort_order).unwrap_or(SortOrder::Ascending);

                let cursor_for = |cursor_type: CursorType, id: GraphqlId| {
                    let model_id = model.fields().id();
                    let where_condition = model_id.as_column().equals(id.clone());

                    let select_query = Select::from_table(model.table())
                        .column(field.as_column())
                        .so_that(ConditionTree::single(where_condition));

                    let compare = match (cursor_type, sort_order) {
                        (CursorType::Before, SortOrder::Ascending) => field
                            .as_column()
                            .equals(select_query.clone())
                            .and(model_id.as_column().less_than(id))
                            .or(field.as_column().less_than(select_query)),
                        (CursorType::Before, SortOrder::Descending) => field
                            .as_column()
                            .equals(select_query.clone())
                            .and(model_id.as_column().less_than(id))
                            .or(field.as_column().greater_than(select_query)),
                        (CursorType::After, SortOrder::Ascending) => field
                            .as_column()
                            .equals(select_query.clone())
                            .and(model_id.as_column().greater_than(id))
                            .or(field.as_column().greater_than(select_query)),
                        (CursorType::After, SortOrder::Descending) => field
                            .as_column()
                            .equals(select_query.clone())
                            .and(model_id.as_column().greater_than(id))
                            .or(field.as_column().less_than(select_query)),
                    };

                    ConditionTree::single(compare)
                };

                let after_cursor = after
                    .map(|id| {
                        let id_val = id.clone();
                        cursor_for(CursorType::After, id_val)
                    })
                    .unwrap_or(ConditionTree::NoCondition);

                let before_cursor = before
                    .map(|id| {
                        let id_val = id.clone();
                        cursor_for(CursorType::Before, id_val)
                    })
                    .unwrap_or(ConditionTree::NoCondition);

                ConditionTree::and(after_cursor, before_cursor)
            }
        }
    }
}
