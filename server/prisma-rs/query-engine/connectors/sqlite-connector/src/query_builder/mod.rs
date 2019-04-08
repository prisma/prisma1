mod related_nodes;

use crate::{cursor_condition::CursorCondition, filter_conversion::AliasedCondition, ordering::Ordering};
use connector::{Filter, NodeSelector, QueryArguments};
use prisma_models::prelude::*;
use prisma_query::ast::*;
use related_nodes::RelatedNodesQueryBuilder;
use std::sync::Arc;

pub trait SelectDefinition {
    fn into_select(self, _: ModelRef) -> Select;
}

impl SelectDefinition for Filter {
    fn into_select(self, model: ModelRef) -> Select {
        let args = QueryArguments::from(self);
        args.into_select(model)
    }
}

impl SelectDefinition for NodeSelector {
    fn into_select(self, model: ModelRef) -> Select {
        let args = QueryArguments::from(self);
        args.into_select(model)
    }
}

impl SelectDefinition for Select {
    fn into_select(self, _: ModelRef) -> Select {
        self
    }
}

impl SelectDefinition for QueryArguments {
    fn into_select(self, model: ModelRef) -> Select {
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
    pub fn get_nodes<T>(model: ModelRef, selected_fields: &SelectedFields, query: T) -> Select
    where
        T: SelectDefinition,
    {
        selected_fields
            .columns()
            .into_iter()
            .fold(query.into_select(model), |acc, col| acc.column(col.clone()))
    }

    pub fn get_related_nodes(
        from_field: RelationFieldRef,
        from_node_ids: &[GraphqlId],
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> Select {
        let is_with_pagination = query_arguments.is_with_pagination();
        let builder = RelatedNodesQueryBuilder::new(from_field, from_node_ids, query_arguments, selected_fields);

        let select_ast = if is_with_pagination {
            builder.with_pagination()
        } else {
            builder.without_pagination()
        };

        select_ast
    }

    pub fn get_scalar_list_values_by_node_ids(list_field: ScalarFieldRef, node_ids: Vec<GraphqlId>) -> Select {
        let model = list_field.model();
        let table_name = format!("{}_{}", model.db_name(), list_field.name);

        // I vant to suk your blaad... - Vlad the Impaler
        let vhere = "nodeId".in_selection(node_ids);

        let query = Select::from_table(table_name)
            .column("nodeId")
            .column("position")
            .column("value")
            .so_that(vhere);

        query
    }

    pub fn count_by_model(model: ModelRef, query_arguments: QueryArguments) -> Select {
        let id_field = model.fields().id();

        let mut selected_fields = SelectedFields::default();
        selected_fields.add_scalar(id_field.clone(), false);

        let base_query = Self::get_nodes(model, &selected_fields, query_arguments);

        let table = Table::from(base_query).alias("sub");
        let column = Column::from(("sub", id_field.db_name()));
        let select_ast = Select::from_table(table).value(count(column));

        select_ast
    }

    pub fn count_by_table(database: &str, table: &str) -> Select {
        Select::from_table((database, table)).value(count(asterisk()))
    }
}
