use crate::{models::prelude::*, protobuf::prelude::*, PrismaResult};
use sql::prelude::*;
use std::sync::Arc;

pub type OrderVec = Vec<(Column, Option<Order>)>;

pub struct Ordering;

/// Tooling for generating orderings for different query types.
impl Ordering {
    pub fn for_model(
        model: &Model,
        order_by: &Option<OrderBy>,
        reverse: bool,
    ) -> PrismaResult<OrderVec> {
        let first = match order_by {
            Some(ref order) => Some(model.fields().find_from_scalar(&order.scalar_field)?),
            None => None,
        };

        Self::by_fields(first, model.fields().id(), order_by, reverse)
    }

    fn by_fields(
        first_field: Option<Arc<ScalarField>>,
        second_field: Arc<ScalarField>,
        order_by: &Option<OrderBy>,
        reverse: bool,
    ) -> PrismaResult<OrderVec> {
        let default_order = order_by
            .as_ref()
            .map(|order| order.sort_order())
            .unwrap_or(SortOrder::Asc);

        let ordering = match first_field {
            Some(ref first) if first.db_name() != second_field.db_name() => {
                match (default_order, reverse) {
                    (SortOrder::Asc, true) => vec![
                        first.model_column().descend(),
                        second_field.model_column().descend(),
                    ],
                    (SortOrder::Desc, true) => vec![
                        first.model_column().ascend(),
                        second_field.model_column().descend(),
                    ],
                    (SortOrder::Asc, false) => vec![
                        first.model_column().ascend(),
                        second_field.model_column().ascend(),
                    ],
                    (SortOrder::Desc, false) => vec![
                        first.model_column().descend(),
                        second_field.model_column().ascend(),
                    ],
                }
            }
            _ => match (default_order, reverse) {
                (SortOrder::Asc, true) => vec![second_field.model_column().descend()],
                (SortOrder::Desc, true) => vec![second_field.model_column().ascend()],
                (SortOrder::Asc, false) => vec![second_field.model_column().ascend()],
                (SortOrder::Desc, false) => vec![second_field.model_column().descend()],
            },
        };

        Ok(ordering)
    }
}
