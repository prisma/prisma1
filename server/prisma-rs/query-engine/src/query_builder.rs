use crate::cursor_condition::CursorCondition;
use crate::node_selector::NodeSelector;
use crate::ordering::Ordering;
use crate::protobuf::prelude::*;
use crate::protobuf::IntoFilter;
use prisma_models::prelude::*;
use prisma_query::ast::*;

pub struct QueryBuilder;

impl QueryBuilder {
    pub fn get_node_by_where(node_selector: NodeSelector, selected_fields: &SelectedFields) -> (String, Select) {
        let condition = ConditionTree::single(node_selector.field.as_column().equals(node_selector.value));
        let base_query = Select::from(node_selector.field.model().table())
            .so_that(condition)
            .offset(0);

        let select_ast = selected_fields
            .columns()
            .into_iter()
            .fold(base_query, |acc, column| acc.column(column.clone()));

        let db_name = node_selector.field.schema().db_name.clone();

        (db_name, select_ast)
    }

    pub fn get_nodes(
        model: ModelRef,
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> (String, Select) {
        let cursor: ConditionTree = CursorCondition::build(&query_arguments, &model);
        let order_by = query_arguments.order_by.map(|oby| oby.into_order_by(model.clone()));
        let ordering = Ordering::for_model(&model, order_by.as_ref(), query_arguments.last.is_some());

        let filter: ConditionTree = query_arguments
            .filter
            .map(|filter| filter.into_filter(model.clone()))
            .map(|filter| filter.into())
            .unwrap_or(ConditionTree::NoCondition);

        let conditions = ConditionTree::and(filter, cursor);

        let (skip, limit) = match query_arguments.last.or(query_arguments.first) {
            Some(c) => (query_arguments.skip.unwrap_or(0), Some(c + 1)), // +1 to see if there's more data
            None => (query_arguments.skip.unwrap_or(0), None),
        };

        let select_ast = Select::from(model.table()).so_that(conditions).offset(skip as usize);
        let select_ast = ordering.into_iter().fold(select_ast, |acc, ord| acc.order_by(ord));

        let db_name = model.schema().db_name.clone();

        let select_ast = selected_fields
            .columns()
            .into_iter()
            .fold(select_ast, |acc, col| acc.column(col.clone()));

        match limit {
            Some(limit) => (db_name, select_ast.limit(limit as usize)),
            None => (db_name, select_ast),
        }
    }
}
