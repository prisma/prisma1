use crate::mutaction::NestedActions;
use connector::{filter::NodeSelector, ConnectorResult};
use prisma_models::{GraphqlId, RelationFieldRef};
use rusqlite::Transaction;

pub trait DatabaseRelation {
    fn execute_connect(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selector: &NodeSelector,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()>;

    fn execute_disconnect(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selector: &Option<NodeSelector>,
    ) -> ConnectorResult<()>;

    fn execute_set(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selectors: &Vec<NodeSelector>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()>;
}
