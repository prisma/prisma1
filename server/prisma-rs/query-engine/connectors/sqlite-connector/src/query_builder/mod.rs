mod related_nodes;

use crate::cursor_condition::CursorCondition;
use crate::filter_conversion as convert;
use crate::ordering::Ordering;
use connector::{NodeSelector, QueryArguments};
use prisma_models::prelude::*;
use prisma_query::ast::*;
use related_nodes::RelatedNodesQueryBuilder;

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
        let order_by = query_arguments.order_by;
        let ordering = Ordering::for_model(&model, order_by.as_ref(), query_arguments.last.is_some());

        let filter: ConditionTree = query_arguments
            .filter
            .map(convert::filter_to_condition_tree)
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

    pub fn get_related_nodes(
        from_field: RelationFieldRef,
        from_node_ids: Vec<GraphqlId>,
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> (String, Select) {
        let db_name = from_field.model().schema().db_name.clone();
        let is_with_pagination = query_arguments.is_with_pagination();
        let builder = RelatedNodesQueryBuilder::new(from_field, from_node_ids, query_arguments, selected_fields);

        let select_ast = if is_with_pagination {
            builder.with_pagination()
        } else {
            builder.without_pagination()
        };

        (db_name, select_ast)
    }

    pub fn get_scalar_list_values_by_node_ids(
        list_field: ScalarFieldRef,
        node_ids: Vec<GraphqlId>,
    ) -> (String, Select) {
        let model = list_field.model();
        let db_name = model.schema().db_name.clone();
        let table_name = format!("{}_{}", model.db_name(), list_field.name);

        // I vant to suk your blaad... - Vlad the Impaler
        let vhere = "nodeId".in_selection(node_ids);

        let query = Select::from(table_name)
            .column("nodeId")
            .column("position")
            .column("value")
            .so_that(vhere);

        (db_name, query)
    }

    pub fn count_by_model(model: ModelRef, query_arguments: QueryArguments) -> (String, Select) {
        let id_field = model.fields().id();

        let mut selected_fields = SelectedFields::default();
        selected_fields.add_scalar(id_field.clone());

        let (db_name, base_query) = Self::get_nodes(model, query_arguments, &selected_fields);

        let table = Table::from(base_query).alias("sub");
        let column = Column::from(("sub", id_field.db_name()));
        let select_ast = Select::from(table).value(count(column));

        (db_name, select_ast)
    }

    pub fn count_by_table(database: &str, table: &str) -> Select {
        Select::from((database, table)).value(count(asterisk()))
    }
}
