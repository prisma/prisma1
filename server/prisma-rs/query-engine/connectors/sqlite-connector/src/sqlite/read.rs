use crate::{query_builder::QueryBuilder, AliasedCondition, DatabaseRead, SelectDefinition, Sqlite};
use connector::{
    error::*,
    filter::{Filter, NodeSelector},
    ConnectorResult,
};
use prisma_models::*;
use prisma_query::{
    ast::*,
    visitor::{self, *},
};
use rusqlite::{Row, Transaction};
use std::sync::Arc;

impl DatabaseRead for Sqlite {
    fn query<F, T, S>(conn: &Transaction, query: S, mut f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
        S: Into<Select>,
    {
        let (query_sql, params) = visitor::Sqlite::build(query.into());

        let res: ConnectorResult<Vec<T>> = conn
            .prepare(&query_sql)?
            .query_map(&params, |row| f(row))?
            .map(|row_res| row_res.unwrap())
            .collect();

        Ok(res?)
    }

    fn count<C, T>(conn: &Transaction, table: T, conditions: C) -> ConnectorResult<usize>
    where
        C: Into<ConditionTree>,
        T: Into<Table>,
    {
        let select = Select::from_table(table)
            .value(count(asterisk()))
            .so_that(conditions.into());

        let (sql, params) = visitor::Sqlite::build(select);

        let res = conn
            .prepare(&sql)?
            .query_map(&params, |row| Self::fetch_int(row))?
            .map(|r| r.unwrap())
            .next()
            .unwrap_or(0);

        Ok(res as usize)
    }

    fn ids_for<T>(conn: &Transaction, model: ModelRef, into_select: T) -> ConnectorResult<Vec<GraphqlId>>
    where
        T: SelectDefinition,
    {
        let select = {
            let selected_fields = SelectedFields::from(model.fields().id());
            QueryBuilder::get_nodes(model, &selected_fields, into_select)
        };

        let ids = Self::query(conn, select, |row| {
            let id: GraphqlId = row.get(0);
            Ok(id)
        })?;

        Ok(ids)
    }

    fn id_for(conn: &Transaction, node_selector: &NodeSelector) -> ConnectorResult<GraphqlId> {
        let model = node_selector.field.model();
        let opt_id = Self::ids_for(conn, model, node_selector.clone())?.into_iter().next();

        opt_id.ok_or_else(|| ConnectorError::NodeNotFoundForWhere(NodeSelectorInfo::from(node_selector)))
    }

    fn find_node(conn: &Transaction, node_selector: &NodeSelector) -> ConnectorResult<SingleNode> {
        let model = node_selector.field.model();
        let selected_fields = SelectedFields::from(Arc::clone(&model));

        let select = QueryBuilder::get_nodes(model, &selected_fields, node_selector);

        let node = Self::query(conn, select, |row| Self::read_row(row, &selected_fields))?
            .into_iter()
            .next()
            .ok_or_else(|| ConnectorError::NodeNotFoundForWhere(NodeSelectorInfo::from(node_selector)))?;

        Ok(SingleNode::new(node, selected_fields.names()))
    }

    fn get_id_by_parent(
        conn: &Transaction,
        parent_field: RelationFieldRef,
        parent_id: &GraphqlId,
        selector: &Option<NodeSelector>,
    ) -> ConnectorResult<GraphqlId> {
        let ids = Self::get_ids_by_parents(conn, Arc::clone(&parent_field), vec![parent_id], selector.clone())?;

        let id = ids
            .into_iter()
            .next()
            .ok_or_else(|| ConnectorError::NodesNotConnected {
                relation_name: parent_field.relation().name.clone(),
                parent_name: parent_field.model().name.clone(),
                parent_where: None,
                child_name: parent_field.related_model().name.clone(),
                child_where: selector.as_ref().map(NodeSelectorInfo::from),
            })?;

        Ok(id)
    }

    fn get_ids_by_parents<T>(
        conn: &Transaction,
        parent_field: RelationFieldRef,
        parent_ids: Vec<&GraphqlId>,
        selector: Option<T>,
    ) -> ConnectorResult<Vec<GraphqlId>>
    where
        T: Into<Filter>,
    {
        let related_model = parent_field.related_model();
        let relation = parent_field.relation();
        let child_id_field = relation.column_for_relation_side(parent_field.relation_side.opposite());
        let parent_id_field = relation.column_for_relation_side(parent_field.relation_side);

        let subselect = Select::from_table(relation.relation_table())
            .column(child_id_field)
            .so_that(parent_id_field.in_selection(parent_ids));

        let conditions = related_model.fields().id().db_name().in_selection(subselect);

        let conditions = match selector {
            Some(into_cond) => {
                let filter: Filter = into_cond.into();
                conditions.and(filter.aliased_cond(None))
            }
            None => conditions.into(),
        };

        let select = Select::from_table(related_model.table())
            .column(related_model.fields().id().as_column())
            .so_that(conditions);

        Self::query(conn, select, |row| {
            let id: GraphqlId = row.get(0);
            Ok(id)
        })
    }
}
