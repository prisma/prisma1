use prisma_models::prelude::*;
use prisma_query::ast::*;

pub type OrderVec = Vec<(DatabaseValue, Option<Order>)>;

pub struct Ordering;

/// Tooling for generating orderings for different query types.
impl Ordering {
    pub fn for_model(model: &Model, order_by: Option<&OrderBy>, reverse: bool) -> OrderVec {
        Self::by_fields(
            order_by.map(|oby| oby.field.as_column()),
            model.fields().id().as_column(),
            order_by,
            reverse,
        )
    }

    pub fn internal<C>(second_field: C, order_by: Option<&OrderBy>, reverse: bool) -> OrderVec
    where
        C: Into<Column>,
    {
        Self::by_fields(
            order_by.map(|oby| oby.field.as_column()),
            second_field.into(),
            order_by,
            reverse,
        )
    }

    pub fn aliased_internal(
        alias: &str,
        secondary_alias: &str,
        secondary_field: &str,
        order_by: Option<&OrderBy>,
        reverse: bool,
    ) -> OrderVec {
        Self::by_fields(
            order_by.map(|oby| (alias, oby.field.db_name()).into()),
            (secondary_alias, secondary_field).into(),
            order_by,
            reverse,
        )
    }

    fn by_fields(
        first_column: Option<Column>,
        second_column: Column,
        order_by: Option<&OrderBy>,
        reverse: bool,
    ) -> OrderVec {
        let default_order = order_by
            .as_ref()
            .map(|order| order.sort_order)
            .unwrap_or(SortOrder::Ascending);

        let ordering = match first_column {
            Some(first) => {
                if first.name != second_column.name {
                    match (default_order, reverse) {
                        (SortOrder::Ascending, true) => vec![first.descend(), second_column.descend()],
                        (SortOrder::Descending, true) => vec![first.ascend(), second_column.descend()],
                        (SortOrder::Ascending, false) => vec![first.ascend(), second_column.ascend()],
                        (SortOrder::Descending, false) => vec![first.descend(), second_column.ascend()],
                    }
                } else {
                    match (default_order, reverse) {
                        (SortOrder::Ascending, true) => vec![second_column.descend()],
                        (SortOrder::Descending, true) => vec![second_column.ascend()],
                        (SortOrder::Ascending, false) => vec![second_column.ascend()],
                        (SortOrder::Descending, false) => vec![second_column.descend()],
                    }
                }
            }
            _ => match (default_order, reverse) {
                (SortOrder::Ascending, true) => vec![second_column.descend()],
                (SortOrder::Descending, true) => vec![second_column.ascend()],
                (SortOrder::Ascending, false) => vec![second_column.ascend()],
                (SortOrder::Descending, false) => vec![second_column.descend()],
            },
        };

        ordering
    }
}
