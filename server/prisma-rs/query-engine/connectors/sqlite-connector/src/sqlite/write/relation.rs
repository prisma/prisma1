use crate::{
    mutaction::{MutationBuilder, NestedActions},
    DatabaseRead, DatabaseRelation, DatabaseWrite, Sqlite,
};
use connector::{filter::NodeSelector, ConnectorResult};
use prisma_models::{GraphqlId, RelationFieldRef};
use rusqlite::Transaction;
use std::sync::Arc;

impl DatabaseRelation for Sqlite {
    fn execute_connect(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selector: &NodeSelector,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()> {
        if let Some((select, check)) = actions.required_check(parent_id)? {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            check.call_box(ids.into_iter().next())?
        }

        let child_id = Self::id_for(conn, node_selector)?;

        if let Some(query) = actions.parent_removal(parent_id) {
            Self::execute_one(conn, query)?;
        }

        if let Some(query) = actions.child_removal(&child_id) {
            Self::execute_one(conn, query)?;
        }

        let relation_query = MutationBuilder::create_relation(relation_field, parent_id, &child_id);
        Self::execute_one(conn, relation_query)?;

        Ok(())
    }

    fn execute_disconnect(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selector: &Option<NodeSelector>,
    ) -> ConnectorResult<()> {
        if let Some((select, check)) = actions.required_check(parent_id)? {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            check.call_box(ids.into_iter().next())?
        }

        match node_selector {
            None => {
                let (select, check) = actions.ensure_parent_is_connected(parent_id);

                let ids = Self::query(conn, select, Self::fetch_id)?;
                check.call_box(ids.into_iter().next())?;

                Self::execute_one(conn, actions.removal_by_parent(parent_id))
            }
            Some(ref selector) => {
                let child_id = Self::id_for(conn, selector)?;
                let (select, check) = actions.ensure_connected(parent_id, &child_id);

                let ids = Self::query(conn, select, Self::fetch_id)?;
                check.call_box(ids.into_iter().next())?;

                Self::execute_one(conn, actions.removal_by_parent_and_child(parent_id, &child_id))
            }
        }
    }

    fn execute_set(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selectors: &Vec<NodeSelector>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()> {
        if let Some((select, check)) = actions.required_check(parent_id)? {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            check.call_box(ids.into_iter().next())?
        }

        Self::execute_one(conn, actions.removal_by_parent(parent_id))?;

        for selector in node_selectors {
            let child_id = Self::id_for(conn, selector)?;
            if !relation_field.is_list {
                Self::execute_one(conn, actions.removal_by_child(&child_id))?;
            }

            let relation_query = MutationBuilder::create_relation(Arc::clone(&relation_field), parent_id, &child_id);
            Self::execute_one(conn, relation_query)?;
        }

        Ok(())
    }
}
