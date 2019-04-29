use super::{create, delete, delete_many, relation, update, update_many};
use crate::Transaction;
use connector::{error::ConnectorError, mutaction::*, ConnectorResult};
use prisma_models::GraphqlId;
use std::sync::Arc;

pub fn execute(conn: &mut Transaction, mutactions: &NestedMutactions, parent_id: &GraphqlId) -> ConnectorResult<()> {
    fn create(conn: &mut Transaction, parent_id: &GraphqlId, cn: &NestedCreateNode) -> ConnectorResult<()> {
        let parent_id = create::execute_nested(
            conn,
            parent_id,
            cn,
            Arc::clone(&cn.relation_field),
            &cn.non_list_args,
            &cn.list_args,
        )?;

        execute(conn, &cn.nested_mutactions, &parent_id)?;

        Ok(())
    }

    fn update(conn: &mut Transaction, parent_id: &GraphqlId, un: &NestedUpdateNode) -> ConnectorResult<()> {
        let parent_id = update::execute_nested(
            conn,
            parent_id,
            &un.where_,
            Arc::clone(&un.relation_field),
            &un.non_list_args,
            &un.list_args,
        )?;

        execute(conn, &un.nested_mutactions, &parent_id)?;

        Ok(())
    }

    for create_node in mutactions.creates.iter() {
        create(conn, parent_id, create_node)?;
    }

    for update_node in mutactions.updates.iter() {
        update(conn, parent_id, update_node)?;
    }

    for upsert_node in mutactions.upserts.iter() {
        let id_opt = conn.find_id_by_parent(Arc::clone(&upsert_node.relation_field), parent_id, &upsert_node.where_);

        match id_opt {
            Ok(_) => update(conn, parent_id, &upsert_node.update)?,
            Err(_e @ ConnectorError::NodesNotConnected { .. }) => create(conn, parent_id, &upsert_node.create)?,
            Err(e) => return Err(e),
        }
    }

    for delete_node in mutactions.deletes.iter() {
        delete::execute_nested(
            conn,
            parent_id,
            delete_node,
            &delete_node.where_,
            Arc::clone(&delete_node.relation_field),
        )?;
    }

    for connect in mutactions.connects.iter() {
        relation::connect(
            conn,
            &parent_id,
            connect,
            &connect.where_,
            Arc::clone(&connect.relation_field),
        )?;
    }

    for set in mutactions.sets.iter() {
        relation::set(conn, &parent_id, set, &set.wheres, Arc::clone(&set.relation_field))?;
    }

    for disconnect in mutactions.disconnects.iter() {
        relation::disconnect(conn, &parent_id, disconnect, &disconnect.where_)?;
    }

    for update_many in mutactions.update_manys.iter() {
        update_many::execute_nested(
            conn,
            &parent_id,
            &update_many.filter,
            Arc::clone(&update_many.relation_field),
            &update_many.non_list_args,
            &update_many.list_args,
        )?;
    }

    for delete_many in mutactions.delete_manys.iter() {
        delete_many::execute_nested(
            conn,
            &parent_id,
            &delete_many.filter,
            Arc::clone(&delete_many.relation_field),
        )?;
    }

    Ok(())
}
