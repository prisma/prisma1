use crate::node_selector::NodeSelector;
use prisma_models::SelectedFields;
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
}
