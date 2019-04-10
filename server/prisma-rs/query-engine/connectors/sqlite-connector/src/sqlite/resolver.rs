use crate::{query_builder::QueryBuilder, DatabaseRead, Sqlite, TransactionalExecutor};
use connector::{filter::NodeSelector, *};
use itertools::Itertools;
use prisma_models::*;

impl DataResolver for Sqlite {
    fn get_node_by_where(
        &self,
        node_selector: NodeSelector,
        selected_fields: &SelectedFields,
    ) -> ConnectorResult<Option<SingleNode>> {
        let db_name = &node_selector.field.model().schema().db_name;
        let query = QueryBuilder::get_nodes(node_selector.field.model(), selected_fields, node_selector);
        let field_names = selected_fields.names();

        let nodes = self.with_transaction(db_name, |conn| {
            Self::query(conn, query, |row| Sqlite::read_row(row, selected_fields))
        })?;

        let result = nodes.into_iter().next().map(|node| SingleNode { node, field_names });

        Ok(result)
    }

    fn get_nodes(
        &self,
        model: ModelRef,
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> ConnectorResult<ManyNodes> {
        let db_name = &model.schema().db_name;
        let field_names = selected_fields.names();
        let query = QueryBuilder::get_nodes(model, selected_fields, query_arguments);

        let nodes = self.with_transaction(db_name, |conn| {
            Self::query(conn, query, |row| Sqlite::read_row(row, selected_fields))
        })?;

        Ok(ManyNodes { nodes, field_names })
    }

    fn get_related_nodes(
        &self,
        from_field: RelationFieldRef,
        from_node_ids: &[GraphqlId],
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> ConnectorResult<ManyNodes> {
        let db_name = &from_field.model().schema().db_name;
        let field_names = selected_fields.names();
        let query = QueryBuilder::get_related_nodes(from_field, from_node_ids, query_arguments, selected_fields);

        let nodes = self.with_transaction(db_name, |conn| {
            Self::query(conn, query, |row| {
                let position = field_names.len();

                let mut node = Sqlite::read_row(row, &selected_fields)?;
                node.add_parent_id(row.get(position - 1));

                Ok(node)
            })
        })?;

        Ok(ManyNodes { nodes, field_names })
    }

    fn count_by_model(&self, model: ModelRef, query_arguments: QueryArguments) -> ConnectorResult<usize> {
        let db_name = &model.schema().db_name;
        let table = model.table();
        let query = QueryBuilder::count_by_model(model, query_arguments);

        self.with_transaction(db_name, |conn| Self::count(conn, table, query))
    }

    fn count_by_table(&self, database: &str, table: &str) -> ConnectorResult<usize> {
        let query = QueryBuilder::count_by_table(database, table);
        self.with_transaction(database, |conn| Self::count(conn, table, query))
    }

    fn get_scalar_list_values_by_node_ids(
        &self,
        list_field: ScalarFieldRef,
        node_ids: Vec<GraphqlId>,
    ) -> ConnectorResult<Vec<ScalarListValues>> {
        let db_name = &list_field.model().schema().db_name;
        let type_identifier = list_field.type_identifier;
        let query = QueryBuilder::get_scalar_list_values_by_node_ids(list_field, node_ids);

        let results = self.with_transaction(db_name, |conn| {
            Self::query(conn, query, |row| {
                let node_id: GraphqlId = row.get(0);
                let value: PrismaValue = Sqlite::fetch_value(type_identifier, row, 2)?;

                Ok(ScalarListElement { node_id, value })
            })
        })?;

        let mut list_values = Vec::new();

        for (node_id, elements) in &results.into_iter().group_by(|ele| ele.node_id.clone()) {
            let values = ScalarListValues {
                node_id,
                values: elements.into_iter().map(|e| e.value).collect(),
            };
            list_values.push(values);
        }

        Ok(list_values)
    }
}

struct ScalarListElement {
    node_id: GraphqlId,
    value: PrismaValue,
}
