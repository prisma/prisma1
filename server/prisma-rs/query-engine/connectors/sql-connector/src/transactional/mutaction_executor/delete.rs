use crate::{
    mutaction::{DeleteActions, MutationBuilder, NestedActions},
    Transaction,
};
use connector::{
    error::{ConnectorError, NodeSelectorInfo},
    filter::NodeSelector,
    ConnectorResult,
};
use prisma_models::{GraphqlId, RelationFieldRef, SingleNode};
use std::sync::Arc;

/// A top level delete that removes one record. Violating any relations or a
/// non-existing record will cause an error.
///
/// Will return the deleted record if the delete was successful.
pub fn execute(conn: &mut Transaction, node_selector: &NodeSelector) -> ConnectorResult<SingleNode> {
    let model = node_selector.field.model();
    let record = conn.find_record(node_selector)?;
    let id = record.get_id_value(Arc::clone(&model)).unwrap();

    DeleteActions::check_relation_violations(Arc::clone(&model), &[id], |select| {
        let ids = conn.select_ids(select)?;
        Ok(ids.into_iter().next())
    })?;

    for delete in MutationBuilder::delete_many(model, &[id]) {
        conn.delete(delete)?;
    }

    Ok(record)
}

/// A nested delete that removes one item related to the given `parent_id`.
/// If no `RecordFinder` is given, will delete the first item from the
/// table.
///
/// Errors thrown from domain violations:
///
/// - Violating any relations where the deleted record is required
/// - If the deleted record is not connected to the parent
/// - The record does not exist
pub fn execute_nested(
    conn: &mut Transaction,
    parent_id: &GraphqlId,
    actions: &NestedActions,
    node_selector: &Option<NodeSelector>,
    relation_field: RelationFieldRef,
) -> ConnectorResult<()> {
    if let Some(ref node_selector) = node_selector {
        conn.find_id(node_selector)?;
    };

    let child_id = conn
        .find_id_by_parent(Arc::clone(&relation_field), parent_id, node_selector)
        .map_err(|e| match e {
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
        })?;

    {
        let (select, check) = actions.ensure_connected(parent_id, &child_id);
        let ids = conn.select_ids(select)?;
        check.call_box(ids.into_iter().next().is_some())?;
    }

    let related_model = relation_field.related_model();

    DeleteActions::check_relation_violations(related_model, &[&child_id; 1], |select| {
        let ids = conn.select_ids(select)?;
        Ok(ids.into_iter().next())
    })?;

    for delete in MutationBuilder::delete_many(relation_field.related_model(), &[&child_id]) {
        conn.delete(delete)?;
    }

    Ok(())
}
