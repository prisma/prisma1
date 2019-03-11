use crate::protobuf::prelude::*;
use prisma_models::prelude::*;
use prisma_query::ast::*;

#[derive(Clone, Copy)]
enum CursorType {
    Before,
    After,
}

pub struct CursorCondition;

impl From<IdValue> for DatabaseValue {
    fn from(id: IdValue) -> DatabaseValue {
        match id {
            IdValue::String(s) => s.into(),
            IdValue::Int(i) => i.into(),
            IdValue::Uuid(s) => s.into(), // todo: this is probably not the correct handling for UUIDs
        }
    }
}

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
                    Some(order) => model.fields().find_from_scalar(&order.scalar_field).unwrap(),
                    None => model.fields().id(),
                };

                let sort_order: Order = order_by.map(|order| order.sort_order().into()).unwrap_or(Order::Asc);

                let cursor_for = |cursor_type: CursorType, id: IdValue| {
                    let model_id = model.fields().id();
                    let where_condition = model_id.as_column().equals(id.clone());

                    let select_query = Select::from(model.table())
                        .column(field.as_column())
                        .column(model_id.as_column())
                        .so_that(ConditionTree::single(where_condition));

                    let compare = match (cursor_type, sort_order) {
                        (CursorType::Before, Order::Asc) => field
                            .as_column()
                            .equals(select_query.clone())
                            .and(model_id.as_column().less_than(id))
                            .or(field.as_column().less_than(select_query)),
                        (CursorType::Before, Order::Desc) => field
                            .as_column()
                            .equals(select_query.clone())
                            .and(model_id.as_column().less_than(id))
                            .or(field.as_column().greater_than(select_query)),
                        (CursorType::After, Order::Asc) => field
                            .as_column()
                            .equals(select_query.clone())
                            .and(model_id.as_column().greater_than(id))
                            .or(field.as_column().greater_than(select_query)),
                        (CursorType::After, Order::Desc) => field
                            .as_column()
                            .equals(select_query.clone())
                            .and(model_id.as_column().greater_than(id))
                            .or(field.as_column().less_than(select_query)),
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
