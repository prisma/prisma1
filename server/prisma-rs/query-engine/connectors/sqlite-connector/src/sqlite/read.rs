use crate::{query_builder::QueryBuilder, DatabaseRead, SelectDefinition, Sqlite};
use connector::{error::*, filter::NodeSelector, ConnectorResult};
use prisma_models::*;
use prisma_query::{
    ast::*,
    visitor::{self, *},
};
use rusqlite::{Row, Transaction};

impl DatabaseRead for Sqlite {
    fn query<F, T, S>(conn: &Transaction, query: S, mut f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
        S: Into<Select>,
    {
        let (query_sql, params) = dbg!(visitor::Sqlite::build(query.into()));

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

        let (sql, params) = dbg!(visitor::Sqlite::build(select));

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

    fn id_for(conn: &Transaction, model: ModelRef, node_selector: &NodeSelector) -> ConnectorResult<GraphqlId> {
        let opt_id = Self::ids_for(conn, model, node_selector.clone())?.into_iter().next();

        opt_id.ok_or_else(|| ConnectorError::NodeNotFoundForWhere(NodeSelectorInfo::from(node_selector)))
    }

    fn get_ids_by_parents(
        conn: &Transaction,
        parent_field: RelationFieldRef,
        parent_ids: Vec<GraphqlId>,
        selector: &Option<NodeSelector>,
    ) -> ConnectorResult<Vec<GraphqlId>> {
        let related_model = parent_field.related_model();
        let relation = parent_field.relation();
        let child_id_field = relation.column_for_relation_side(parent_field.relation_side.opposite());
        let parent_id_field = relation.column_for_relation_side(parent_field.relation_side);

        let subselect = Select::from_table(relation.relation_table())
            .column(child_id_field)
            .so_that(parent_id_field.in_selection(parent_ids));

        let conditions = related_model.fields().id().db_name().in_selection(subselect);

        let conditions = match selector {
            Some(ref node_selector) => {
                conditions.and(node_selector.field.as_column().equals(node_selector.value.clone()))
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
