mod many_related_records;

use crate::{cursor_condition::CursorCondition, filter_conversion::AliasedCondition, ordering::Ordering};
use connector::{
    filter::{Filter, RecordFinder},
    QueryArguments,
};
use prisma_models::prelude::*;
use prisma_query::ast::*;
use std::sync::Arc;

pub use many_related_records::*;

pub trait SelectDefinition {
    fn into_select(self, _: ModelRef) -> Select<'static>;
}

impl SelectDefinition for Filter {
    fn into_select(self, model: ModelRef) -> Select<'static> {
        let args = QueryArguments::from(self);
        args.into_select(model)
    }
}

impl SelectDefinition for RecordFinder {
    fn into_select(self, model: ModelRef) -> Select<'static> {
        let args = QueryArguments::from(self);
        args.into_select(model)
    }
}

impl SelectDefinition for &RecordFinder {
    fn into_select(self, model: ModelRef) -> Select<'static> {
        self.clone().into_select(model)
    }
}

impl SelectDefinition for Select<'static> {
    fn into_select(self, _: ModelRef) -> Select<'static> {
        self
    }
}

impl SelectDefinition for QueryArguments {
    fn into_select(self, model: ModelRef) -> Select<'static> {
        let cursor: ConditionTree = CursorCondition::build(&self, Arc::clone(&model));
        let order_by = self.order_by;
        let ordering = Ordering::for_model(Arc::clone(&model), order_by.as_ref(), self.last.is_some());

        let filter: ConditionTree = self
            .filter
            .map(|f| f.aliased_cond(None))
            .unwrap_or(ConditionTree::NoCondition);

        let conditions = match (filter, cursor) {
            (ConditionTree::NoCondition, cursor) => cursor,
            (filter, ConditionTree::NoCondition) => filter,
            (filter, cursor) => ConditionTree::and(filter, cursor),
        };

        let (skip, limit) = match self.last.or(self.first) {
            Some(c) => (self.skip.unwrap_or(0), Some(c + 1)), // +1 to see if there's more data
            None => (self.skip.unwrap_or(0), None),
        };

        let select_ast = Select::from_table(model.table())
            .so_that(conditions)
            .offset(skip as usize);

        let select_ast = ordering.into_iter().fold(select_ast, |acc, ord| acc.order_by(ord));

        match limit {
            Some(limit) => select_ast.limit(limit as usize),
            None => select_ast,
        }
    }
}

pub struct QueryBuilder;

impl QueryBuilder {
    pub fn get_records<T>(model: ModelRef, selected_fields: &SelectedFields, query: T) -> Select<'static>
    where
        T: SelectDefinition,
    {
        selected_fields
            .columns()
            .into_iter()
            .fold(query.into_select(model), |acc, col| acc.column(col.clone()))
    }

    pub fn get_scalar_list_values_by_record_ids(
        list_field: ScalarFieldRef,
        record_ids: Vec<GraphqlId>,
    ) -> Select<'static> {
        let table = list_field.scalar_list_table().table();

        // I vant to saak your blaad... - Vlad the Impaler
        let vhere = "nodeId".in_selection(record_ids);

        let query = Select::from_table(table)
            .column("nodeId")
            .column("value")
            .so_that(vhere);

        query
    }

    pub fn count_by_model(model: ModelRef, query_arguments: QueryArguments) -> Select<'static> {
        let id_field = model.fields().id();

        let mut selected_fields = SelectedFields::default();
        selected_fields.add_scalar(id_field.clone(), false);

        let base_query = Self::get_records(model, &selected_fields, query_arguments);

        let table = Table::from(base_query).alias("sub");
        let column = Column::from(("sub", id_field.db_name().to_string()));
        let select_ast = Select::from_table(table).value(count(column));

        select_ast
    }

    pub fn count_by_table(database: &str, table: &str) -> Select<'static> {
        Select::from_table((database.to_string(), table.to_string())).value(count(asterisk()))
    }
}
