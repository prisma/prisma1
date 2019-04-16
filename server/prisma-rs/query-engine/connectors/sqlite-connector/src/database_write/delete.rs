use crate::mutaction::NestedActions;
use connector::{
    filter::{Filter, NodeSelector},
    ConnectorResult,
};
use prisma_models::{GraphqlId, ModelRef, ProjectRef, RelationFieldRef, SingleNode};
use rusqlite::Transaction;

pub trait DatabaseDelete {
    fn execute_delete(conn: &Transaction, node_selector: &NodeSelector) -> ConnectorResult<SingleNode>;
    fn execute_delete_many(conn: &Transaction, model: ModelRef, filter: &Filter) -> ConnectorResult<usize>;

    fn execute_nested_delete(
        conn: &Transaction,
        parent_id: &GraphqlId,
        nested_actions: &NestedActions,
        node_selector: &Option<NodeSelector>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()>;

    fn execute_nested_delete_many(
        conn: &Transaction,
        parent_id: &GraphqlId,
        filter: &Option<Filter>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<usize>;

    fn execute_reset_data(conn: &Transaction, project: ProjectRef) -> ConnectorResult<()>;
}
