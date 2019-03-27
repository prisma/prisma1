use crate::{database_executor::DatabaseExecutor, query_builder::QueryBuilder, sqlite::Sqlite};
use connector::*;
use itertools::Itertools;
use prisma_models::prelude::*;
use std::sync::Arc;

pub struct SqlResolver<T>
where
    T: DatabaseExecutor,
{
    database_executor: Arc<T>,
}

impl<T> SqlResolver<T>
where
    T: DatabaseExecutor,
{
    pub fn new(database_executor: Arc<T>) -> Self {
        Self { database_executor }
    }
}

impl DataResolver for SqlResolver<Sqlite> {
    fn get_node_by_where(
        &self,
        node_selector: NodeSelector,
        selected_fields: &SelectedFields,
    ) -> ConnectorResult<Option<SingleNode>> {
        let (db_name, query) = QueryBuilder::get_nodes(node_selector.field.model(), node_selector, selected_fields);

        let scalar_fields = selected_fields.scalar_non_list();
        let field_names = scalar_fields.iter().map(|f| f.name.clone()).collect();

        let nodes = self
            .database_executor
            .with_rows(query, db_name, |row| Sqlite::read_row(row, &selected_fields))?;

        let result = nodes.into_iter().next().map(|node| SingleNode { node, field_names });

        Ok(result)
    }

    fn get_nodes(
        &self,
        model: ModelRef,
        query_arguments: QueryArguments,
        selected_fields: SelectedFields,
    ) -> ConnectorResult<ManyNodes> {
        let scalar_fields = selected_fields.scalar_non_list();
        let field_names = scalar_fields.iter().map(|f| f.name.clone()).collect();
        let (db_name, query) = QueryBuilder::get_nodes(model, query_arguments, &selected_fields);

        let nodes = self
            .database_executor
            .with_rows(query, db_name, |row| Sqlite::read_row(row, &selected_fields))?;

        Ok(ManyNodes { nodes, field_names })
    }

    fn get_related_nodes(
        &self,
        from_field: RelationFieldRef,
        from_node_ids: &[GraphqlId],
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> ConnectorResult<ManyNodes> {
        let scalar_fields = selected_fields.scalar_non_list();
        let field_names = scalar_fields.iter().map(|f| f.name.clone()).collect();
        let (db_name, query) =
            QueryBuilder::get_related_nodes(from_field, from_node_ids, query_arguments, selected_fields);

        let nodes = self.database_executor.with_rows(query, db_name, |row| {
            let mut node = Sqlite::read_row(row, &selected_fields)?;
            let position = scalar_fields.len();

            node.add_related_id(row.get(position));
            node.add_parent_id(row.get(position + 1));
            Ok(node)
        })?;

        Ok(ManyNodes { nodes, field_names })
    }

    fn count_by_model(&self, model: ModelRef, query_arguments: QueryArguments) -> ConnectorResult<usize> {
        let (db_name, query) = QueryBuilder::count_by_model(model, query_arguments);

        let res = self
            .database_executor
            .with_rows(query, db_name, |row| Ok(Sqlite::fetch_int(row)))?
            .into_iter()
            .next()
            .unwrap_or(0);

        Ok(res as usize)
    }

    fn count_by_table(&self, database: &str, table: &str) -> ConnectorResult<usize> {
        let query = QueryBuilder::count_by_table(database, table);

        let res = self
            .database_executor
            .with_rows(query, String::from(database), |row| Ok(Sqlite::fetch_int(row)))?
            .into_iter()
            .next()
            .unwrap_or(0);

        Ok(res as usize)
    }

    fn get_scalar_list_values_by_node_ids(
        &self,
        list_field: ScalarFieldRef,
        node_ids: Vec<GraphqlId>,
    ) -> ConnectorResult<Vec<ScalarListValues>> {
        let type_identifier = list_field.type_identifier;
        let (db_name, query) = QueryBuilder::get_scalar_list_values_by_node_ids(list_field, node_ids);

        let results = self.database_executor.with_rows(query, db_name, |row| {
            let node_id: GraphqlId = row.get(0);
            let _position: u32 = row.get(1);
            let value: PrismaValue = Sqlite::fetch_value(type_identifier, row, 2)?;

            Ok(ScalarListElement {
                node_id,
                _position,
                value,
            })
        })?;

        let mut list_values = vec![];
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

// TODO: Check do we need the position at all.
#[allow(dead_code)]
struct ScalarListElement {
    node_id: GraphqlId,
    _position: u32,
    value: PrismaValue,
}
