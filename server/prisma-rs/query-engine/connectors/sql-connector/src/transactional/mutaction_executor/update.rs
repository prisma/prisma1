use crate::{mutaction::MutationBuilder, Transaction};
use connector::{filter::NodeSelector, ConnectorResult};
use prisma_models::{GraphqlId, ModelRef, PrismaArgs, PrismaListValue, RelationFieldRef};
use std::sync::Arc;

/// Updates one record and any associated list record in the database.
pub fn execute<S>(
    conn: &mut Transaction,
    node_selector: &NodeSelector,
    non_list_args: &PrismaArgs,
    list_args: &[(S, PrismaListValue)],
) -> ConnectorResult<GraphqlId>
where
    S: AsRef<str>,
{
    let model = node_selector.field.model();
    let id = conn.find_id(node_selector)?;

    if let Some(update) = MutationBuilder::update_one(Arc::clone(&model), &id, non_list_args)? {
        conn.update(update)?;
    }

    update_list_args(conn, &[id.clone()], Arc::clone(&model), list_args)?;

    Ok(id)
}

/// Updates a nested item related to the parent, including any associated
/// list values.
pub fn execute_nested<S>(
    conn: &mut Transaction,
    parent_id: &GraphqlId,
    node_selector: &Option<NodeSelector>,
    relation_field: RelationFieldRef,
    non_list_args: &PrismaArgs,
    list_args: &[(S, PrismaListValue)],
) -> ConnectorResult<GraphqlId>
where
    S: AsRef<str>,
{
    if let Some(ref node_selector) = node_selector {
        conn.find_id(node_selector)?;
    };

    let id = conn.find_id_by_parent(Arc::clone(&relation_field), parent_id, node_selector)?;
    let node_selector = NodeSelector::from((relation_field.related_model().fields().id(), id));

    execute(conn, &node_selector, non_list_args, list_args)
}

/// Updates list args related to the given records.
pub fn update_list_args<S>(
    conn: &mut Transaction,
    ids: &[GraphqlId],
    model: ModelRef,
    list_args: &[(S, PrismaListValue)],
) -> ConnectorResult<()>
where
    S: AsRef<str>,
{
    for (field_name, list_value) in list_args {
        let field = model.fields().find_from_scalar(field_name.as_ref()).unwrap();
        let table = field.scalar_list_table();
        let (deletes, inserts) = MutationBuilder::update_scalar_list_values(&table, &list_value, ids.to_vec());

        for delete in deletes {
            conn.delete(delete)?;
        }

        for insert in inserts {
            conn.insert(insert)?;
        }
    }

    Ok(())
}
