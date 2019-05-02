mod data_resolver;
mod mutaction_executor;

pub use data_resolver::*;
pub use mutaction_executor::*;

use crate::{query_builder::QueryBuilder, AliasedCondition, PrismaRow};
use connector::{
    error::*,
    filter::{Filter, NodeSelector},
    ConnectorResult,
};
use prisma_models::*;
use prisma_query::ast::*;
use std::{convert::TryFrom, sync::Arc};

/// A `Transactional` presents a database able to spawn transactions, execute
/// queries in the transaction and commit the results to the database or do a
/// rollback in case of an error.
pub trait Transactional {
    /// Wrap a closure into a transaction. All actions done through the
    /// `Transaction` are commited automatically, or rolled back in case of any
    /// error.
    fn with_transaction<F, T>(&self, db: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&mut Transaction) -> ConnectorResult<T>;
}

pub struct WriteItems {
    pub count: usize,
    pub last_id: usize,
}

/// Abstraction of a database transaction. Start, commit and rollback should be
/// handled per-database basis, `Transaction` providing a minimal interface over
/// different databases.
pub trait Transaction {
    /// Burn them. BURN THEM ALL!
    fn truncate(&mut self, project: ProjectRef) -> ConnectorResult<()>;

    /// Write to the database, returning the change count and last id inserted.
    fn write(&mut self, q: Query) -> ConnectorResult<WriteItems>;

    /// Select multiple rows from the database.
    fn filter(&mut self, q: Select, idents: &[TypeIdentifier]) -> ConnectorResult<Vec<PrismaRow>>;

    /// Insert to the database. On success returns the last insert row id.
    fn insert(&mut self, q: Insert) -> ConnectorResult<usize> {
        Ok(self.write(q.into())?.last_id)
    }

    /// Update the database. On success returns the number of rows updated.
    fn update(&mut self, q: Update) -> ConnectorResult<usize> {
        Ok(self.write(q.into())?.count)
    }

    /// Delete from the database. On success returns the number of rows deleted.
    fn delete(&mut self, q: Delete) -> ConnectorResult<usize> {
        Ok(self.write(q.into())?.count)
    }

    /// Find one full record selecting all scalar fields.
    fn find_record(&mut self, node_selector: &NodeSelector) -> ConnectorResult<SingleNode> {
        use ConnectorError::*;

        let model = node_selector.field.model();
        let selected_fields = SelectedFields::from(Arc::clone(&model));
        let select = QueryBuilder::get_nodes(model, &selected_fields, node_selector);
        let idents = selected_fields.type_identifiers();

        let row = self.find(select, idents.as_slice()).map_err(|e| match e {
            NodeDoesNotExist => NodeNotFoundForWhere(NodeSelectorInfo::from(node_selector)),
            e => e,
        })?;

        let node = Node::from(row);

        Ok(SingleNode::new(node, selected_fields.names()))
    }

    /// Select one row from the database.
    fn find(&mut self, q: Select, idents: &[TypeIdentifier]) -> ConnectorResult<PrismaRow> {
        self.filter(q.limit(1), idents)?
            .into_iter()
            .next()
            .ok_or(ConnectorError::NodeDoesNotExist)
    }

    /// Read the first column from the first row as an integer.
    fn find_int(&mut self, q: Select) -> ConnectorResult<i64> {
        // UNWRAP: A dataset will always have at least one column, even if it contains no data.
        let id = self.find(q, &[TypeIdentifier::Int])?.values.into_iter().next().unwrap();

        Ok(i64::try_from(id)?)
    }

    /// Read the first column from the first row as an `GraphqlId`.
    fn find_id(&mut self, node_selector: &NodeSelector) -> ConnectorResult<GraphqlId> {
        let model = node_selector.field.model();
        let filter = Filter::from(node_selector.clone());

        let id = self
            .filter_ids(model, filter)?
            .into_iter()
            .next()
            .ok_or_else(|| ConnectorError::NodeNotFoundForWhere(NodeSelectorInfo::from(node_selector)))?;

        Ok(id)
    }

    /// Read the all columns as an `GraphqlId`
    fn filter_ids(&mut self, model: ModelRef, filter: Filter) -> ConnectorResult<Vec<GraphqlId>> {
        let select = Select::from_table(model.table())
            .column(model.fields().id().as_column())
            .so_that(filter.aliased_cond(None));

        self.select_ids(select)
    }

    fn select_ids(&mut self, select: Select) -> ConnectorResult<Vec<GraphqlId>> {
        let mut rows = self.filter(select, &[TypeIdentifier::GraphQLID])?;
        let mut result = Vec::new();

        for mut row in rows.drain(0..) {
            for value in row.values.drain(0..) {
                result.push(GraphqlId::try_from(value)?)
            }
        }

        Ok(result)
    }

    /// Find a child of a parent. Will return an error if no child found with
    /// the given parameters. A more restrictive version of `get_ids_by_parents`.
    fn find_id_by_parent(
        &mut self,
        parent_field: RelationFieldRef,
        parent_id: &GraphqlId,
        selector: &Option<NodeSelector>,
    ) -> ConnectorResult<GraphqlId> {
        let ids = self.filter_ids_by_parents(
            Arc::clone(&parent_field),
            vec![parent_id],
            selector.clone().map(Filter::from),
        )?;

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

    /// Find all children node id's with the given parent id's, optionally given
    /// a `Filter` for extra filtering.
    fn filter_ids_by_parents(
        &mut self,
        parent_field: RelationFieldRef,
        parent_ids: Vec<&GraphqlId>,
        selector: Option<Filter>,
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
            Some(into_cond) => {
                let filter: Filter = into_cond.into();
                conditions.and(filter.aliased_cond(None))
            }
            None => conditions.into(),
        };

        let select = Select::from_table(related_model.table())
            .column(related_model.fields().id().as_column())
            .so_that(conditions);

        self.select_ids(select)
    }
}
