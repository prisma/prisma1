mod create;
mod delete;
mod relation;
mod update;

pub use create::*;
pub use delete::*;
pub use relation::*;
pub use update::*;

use crate::*;
use connector::{mutaction::*, ConnectorResult};
use prisma_models::GraphqlId;
use prisma_query::{
    ast::Query,
    visitor::{self, Visitor},
};
use rusqlite::Transaction;
use std::sync::Arc;

impl DatabaseWrite for Sqlite {
    fn execute_nested(conn: &Transaction, mutactions: &NestedMutactions, parent_id: &GraphqlId) -> ConnectorResult<()> {
        let create = |create_node: &NestedCreateNode| -> ConnectorResult<()> {
            let parent_id = Self::execute_nested_create(
                conn,
                parent_id,
                create_node,
                Arc::clone(&create_node.relation_field),
                &create_node.non_list_args,
                &create_node.list_args,
            )?;

            Self::execute_nested(conn, &create_node.nested_mutactions, &parent_id)?;

            Ok(())
        };

        let update = |update_node: &NestedUpdateNode| -> ConnectorResult<()> {
            let parent_id = Self::execute_nested_update(
                conn,
                parent_id,
                &update_node.where_,
                Arc::clone(&update_node.relation_field),
                &update_node.non_list_args,
                &update_node.list_args,
            )?;

            Self::execute_nested(conn, &update_node.nested_mutactions, &parent_id)?;

            Ok(())
        };

        for create_node in mutactions.creates.iter() {
            create(create_node)?;
        }

        for update_node in mutactions.updates.iter() {
            update(update_node)?;
        }

        for upsert_node in mutactions.upserts.iter() {
            let ids = Self::get_ids_by_parents(
                conn,
                Arc::clone(&upsert_node.relation_field),
                vec![parent_id],
                upsert_node.where_.clone(),
            )?;

            if ids.is_empty() {
                create(&upsert_node.create)?;
            } else {
                update(&upsert_node.update)?;
            }
        }

        for delete_node in mutactions.deletes.iter() {
            Self::execute_nested_delete(
                conn,
                parent_id,
                delete_node,
                &delete_node.where_,
                Arc::clone(&delete_node.relation_field),
            )?;
        }

        for connect in mutactions.connects.iter() {
            Self::execute_connect(
                conn,
                &parent_id,
                connect,
                &connect.where_,
                Arc::clone(&connect.relation_field),
            )?;
        }

        for set in mutactions.sets.iter() {
            Self::execute_set(conn, &parent_id, set, &set.wheres, Arc::clone(&set.relation_field))?;
        }

        for disconnect in mutactions.disconnects.iter() {
            Self::execute_disconnect(conn, &parent_id, disconnect, &disconnect.where_)?;
        }

        for update_many in mutactions.update_manys.iter() {
            Self::execute_nested_update_many(
                conn,
                &parent_id,
                &update_many.filter,
                Arc::clone(&update_many.relation_field),
                &update_many.non_list_args,
                &update_many.list_args,
            )?;
        }

        for delete_many in mutactions.delete_manys.iter() {
            Self::execute_nested_delete_many(
                conn,
                &parent_id,
                &delete_many.filter,
                Arc::clone(&delete_many.relation_field),
            )?;
        }

        Ok(())
    }

    fn execute_one<T>(conn: &Transaction, query: T) -> ConnectorResult<()>
    where
        T: Into<Query>,
    {
        let (sql, params) = dbg!(visitor::Sqlite::build(query));
        conn.prepare_cached(&sql)?.execute(&params)?;

        Ok(())
    }

    fn execute_many<T>(conn: &Transaction, queries: Vec<T>) -> ConnectorResult<()>
    where
        T: Into<Query>,
    {
        for query in queries {
            Self::execute_one(conn, query)?;
        }

        Ok(())
    }
}
