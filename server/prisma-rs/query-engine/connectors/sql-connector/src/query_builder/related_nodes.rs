use crate::{cursor_condition::CursorCondition, filter_conversion::AliasedCondition, ordering::Ordering};
use connector::QueryArguments;
use prisma_models::prelude::*;
use prisma_query::ast::{
    row_number, Aliasable, Column, Comparable, ConditionTree, Conjuctive, Function, Joinable, Select, Table,
};
use std::sync::Arc;

pub struct RelatedNodesQueryBuilder<'a> {
    from_field: Arc<RelationField>,
    from_node_ids: &'a [GraphqlId],
    selected_fields: &'a SelectedFields,
    conditions: ConditionTree,
    relation: Arc<Relation>,
    related_model: ModelRef,
    window_limits: (u32, u32),
    order_by: Option<OrderBy>,
    cursor_condition: ConditionTree,
    reverse_order: bool,
}

impl<'a> RelatedNodesQueryBuilder<'a> {
    const BASE_TABLE_ALIAS: &'static str = "prismaBaseTableAlias";
    const ROW_NUMBER_ALIAS: &'static str = "prismaRowNumberAlias";
    const ROW_NUMBER_TABLE_ALIAS: &'static str = "prismaRowNumberTableAlias";

    pub fn new(
        from_field: Arc<RelationField>,
        from_node_ids: &'a [GraphqlId],
        query_arguments: QueryArguments,
        selected_fields: &'a SelectedFields,
    ) -> Self {
        let relation = from_field.relation();
        let related_model = from_field.related_model();
        let cursor_condition = CursorCondition::build(&query_arguments, related_model.clone());
        let window_limits = query_arguments.window_limits();

        let order_by: Option<OrderBy> = query_arguments.order_by;
        let conditions: ConditionTree = query_arguments
            .filter
            .map(|f| f.aliased_cond(None))
            .unwrap_or(ConditionTree::NoCondition);

        let reverse_order = query_arguments.last.is_some();

        RelatedNodesQueryBuilder {
            from_field,
            from_node_ids,
            selected_fields,
            conditions,
            relation,
            related_model,
            window_limits,
            order_by,
            cursor_condition,
            reverse_order,
        }
    }

    pub fn with_pagination(self) -> Select {
        let relation_side_column = self.relation_side_column();
        let base_query = self.base_query();
        let cursor_condition = self.cursor_condition;

        let ordering = Ordering::aliased_internal(
            Self::BASE_TABLE_ALIAS,
            Self::BASE_TABLE_ALIAS,
            SelectedFields::RELATED_MODEL_ALIAS,
            self.order_by.as_ref(),
            self.reverse_order,
        );

        // TODO: prisma query crate slice handling
        let conditions = relation_side_column
            .in_selection(self.from_node_ids.to_owned())
            .and(self.conditions)
            .and(cursor_condition);

        let base_with_conditions = match self.order_by {
            Some(order_by) => {
                let column = order_by.field.as_column();

                if self.selected_fields.columns().contains(&column) {
                    base_query.so_that(conditions)
                } else {
                    base_query.column(order_by.field.as_column()).so_that(conditions)
                }
            }
            None => base_query.so_that(conditions),
        };

        let row_number_part: Function = ordering
            .into_iter()
            .fold(row_number(), |acc, ord| acc.order_by(ord))
            .partition_by((Self::BASE_TABLE_ALIAS, SelectedFields::PARENT_MODEL_ALIAS))
            .into();

        let with_row_numbers = Select::from_table(Table::from(base_with_conditions).alias(Self::BASE_TABLE_ALIAS))
            .value(Table::from(Self::BASE_TABLE_ALIAS).asterisk())
            .value(row_number_part.alias(Self::ROW_NUMBER_ALIAS));

        Select::from_table(Table::from(with_row_numbers).alias(Self::ROW_NUMBER_TABLE_ALIAS))
            .value(Table::from(Self::ROW_NUMBER_TABLE_ALIAS).asterisk())
            .so_that(Self::ROW_NUMBER_ALIAS.between(self.window_limits.0 as i64, self.window_limits.1 as i64))
    }

    pub fn without_pagination(self) -> Select {
        let relation_side_column = self.relation_side_column();
        let opposite_relation_side_column = self.opposite_relation_side_column();
        let base_query = self.base_query();
        let cursor_condition = self.cursor_condition;

        // TODO: prisma query crate slice handling
        let conditions = relation_side_column
            .clone()
            .in_selection(self.from_node_ids.to_owned())
            .and(cursor_condition)
            .and(self.conditions);

        Ordering::internal(
            opposite_relation_side_column,
            self.order_by.as_ref(),
            self.reverse_order,
        )
        .into_iter()
        .fold(base_query.so_that(conditions), |acc, ord| acc.order_by(ord))
    }

    fn base_query(&self) -> Select {
        let select = Select::from_table(self.from_field.related_model().table());

        self.selected_fields
            .columns()
            .into_iter()
            .fold(select, |acc, col| acc.column(col.clone()))
            .inner_join(
                self.relation_table()
                    .on(self.id_column().equals(self.opposite_relation_side_column())),
            )
    }

    fn id_column(&self) -> Column {
        self.related_model.id_column()
    }

    fn relation_side_column(&self) -> Column {
        self.relation
            .column_for_relation_side(self.from_field.relation_side)
            .table(Relation::TABLE_ALIAS)
    }

    fn opposite_relation_side_column(&self) -> Column {
        self.relation
            .column_for_relation_side(self.from_field.relation_side.opposite())
            .table(Relation::TABLE_ALIAS)
    }

    fn relation_table(&self) -> Table {
        self.relation.relation_table().alias(Relation::TABLE_ALIAS)
    }
}
