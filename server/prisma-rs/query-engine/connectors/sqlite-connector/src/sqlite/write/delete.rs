use crate::{
    mutaction::{DeleteActions, MutationBuilder, NestedActions},
    DatabaseDelete, DatabaseRead, DatabaseWrite, Sqlite,
};
use connector::{
    error::*,
    filter::{Filter, NodeSelector},
    ConnectorResult,
};
use prisma_models::*;
use rusqlite::Transaction;
use std::sync::Arc;

impl DatabaseDelete for Sqlite {
    fn execute_delete(conn: &Transaction, node_selector: &NodeSelector) -> ConnectorResult<SingleNode> {
        let model = node_selector.field.model();
        let node = Self::find_node(conn, node_selector)?;

        let id = node.get_id_value(Arc::clone(&model)).unwrap();

        DeleteActions::check_relation_violations(Arc::clone(&model), &[id], |select| {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            Ok(ids.into_iter().next())
        })?;

        let deletes = MutationBuilder::delete_many(model, &[id]);
        Self::execute_many(conn, deletes)?;

        Ok(node)
    }

    fn execute_delete_many(conn: &Transaction, model: ModelRef, filter: &Filter) -> ConnectorResult<usize> {
        let ids = Self::ids_for(conn, Arc::clone(&model), filter.clone())?;
        let ids: Vec<&GraphqlId> = ids.iter().map(|id| &*id).collect();
        let count = ids.len();

        DeleteActions::check_relation_violations(Arc::clone(&model), ids.as_slice(), |select| {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            Ok(ids.into_iter().next())
        })?;

        let deletes = { MutationBuilder::delete_many(model, ids.as_slice()) };

        Self::execute_many(conn, deletes)?;

        Ok(count)
    }

    fn execute_nested_delete(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selector: &Option<NodeSelector>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()> {
        if let Some(ref node_selector) = node_selector {
            Self::id_for(conn, node_selector)?;
        };

        let child_id = Self::get_id_by_parent(conn, Arc::clone(&relation_field), parent_id, node_selector).map_err(
            |e| match e {
                ConnectorError::NodesNotConnected {
                    relation_name,
                    parent_name,
                    parent_where: _,
                    child_name,
                    child_where,
                } => {
                    let model = Arc::clone(&relation_field.model());

                    ConnectorError::NodesNotConnected {
                        relation_name: relation_name,
                        parent_name: parent_name,
                        parent_where: Some(NodeSelectorInfo::for_id(model, parent_id)),
                        child_name: child_name,
                        child_where: child_where,
                    }
                }
                e => e,
            },
        )?;

        {
            let (select, check) = actions.ensure_connected(parent_id, &child_id);
            let ids = Self::query(conn, select, Self::fetch_id)?;
            check.call_box(ids.into_iter().next())?;
        }

        let related_model = relation_field.related_model();

        DeleteActions::check_relation_violations(related_model, &[&child_id; 1], |select| {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            Ok(ids.into_iter().next())
        })?;

        let deletes = MutationBuilder::delete_many(relation_field.related_model(), &[&child_id]);
        Self::execute_many(conn, deletes)?;

        Ok(())
    }

    fn execute_nested_delete_many(
        conn: &Transaction,
        parent_id: &GraphqlId,
        filter: &Option<Filter>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<usize> {
        let ids = Self::get_ids_by_parents(conn, Arc::clone(&relation_field), vec![parent_id], filter.clone())?;
        let count = ids.len();

        let ids: Vec<&GraphqlId> = ids.iter().map(|id| &*id).collect();
        let model = relation_field.model();

        DeleteActions::check_relation_violations(model, ids.as_slice(), |select| {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            Ok(ids.into_iter().next())
        })?;

        let deletes = MutationBuilder::delete_many(relation_field.related_model(), ids.as_slice());

        Self::execute_many(conn, deletes)?;

        Ok(count)
    }
}
