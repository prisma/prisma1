mod create;
mod delete;
mod relation;
mod update;

pub use create::*;
pub use delete::*;
pub use relation::*;
pub use update::*;

use crate::*;
use connector::{error::ConnectorError, mutaction::*, ConnectorResult};
use prisma_models::GraphqlId;
use prisma_query::{
    ast::Query,
    visitor::{self, Visitor},
};
use rusqlite::Transaction;
use std::sync::Arc;

impl DatabaseWrite for Sqlite {
    fn execute_toplevel(
        conn: &Transaction,
        mutaction: TopLevelDatabaseMutaction,
    ) -> ConnectorResult<DatabaseMutactionResults> {
        let mut results = DatabaseMutactionResults::default();

        match mutaction {
            TopLevelDatabaseMutaction::CreateNode(ref cn) => {
                let id = Self::execute_create(conn, Arc::clone(&cn.model), &cn.non_list_args, &cn.list_args)?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::Id(id),
                    typ: DatabaseMutactionResultType::Create,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                });
            }
            TopLevelDatabaseMutaction::UpdateNode(ref un) => {
                let id = Self::execute_update(conn, &un.where_, &un.non_list_args, &un.list_args)?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::Id(id),
                    typ: DatabaseMutactionResultType::Update,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                });
            }
            TopLevelDatabaseMutaction::UpsertNode(ref ups) => match Self::id_for(conn, &ups.where_) {
                Err(_e @ ConnectorError::NodeNotFoundForWhere { .. }) => {
                    let create = &ups.create;

                    let id = Self::execute_create(
                        conn,
                        Arc::clone(&create.model),
                        &create.non_list_args.clone(),
                        &create.list_args,
                    )?;

                    results.push(DatabaseMutactionResult {
                        identifier: Identifier::Id(id),
                        typ: DatabaseMutactionResultType::Create,
                        mutaction: DatabaseMutaction::TopLevel(mutaction),
                    });
                }
                Ok(_) => {
                    let id = Self::execute_update(
                        conn,
                        &ups.update.where_,
                        &ups.update.non_list_args,
                        &ups.update.list_args,
                    )?;

                    results.push(DatabaseMutactionResult {
                        identifier: Identifier::Id(id),
                        typ: DatabaseMutactionResultType::Update,
                        mutaction: DatabaseMutaction::TopLevel(mutaction),
                    });
                }
                Err(e) => return Err(e),
            },
            TopLevelDatabaseMutaction::UpdateNodes(ref uns) => {
                let count = Self::execute_update_many(
                    conn,
                    Arc::clone(&uns.model),
                    &uns.filter,
                    &uns.non_list_args,
                    &uns.list_args,
                )?;

                let result = DatabaseMutactionResult {
                    identifier: Identifier::Count(count),
                    typ: DatabaseMutactionResultType::Many,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                };

                results.push(result);
            }
            TopLevelDatabaseMutaction::DeleteNode(ref dn) => {
                let node = Self::execute_delete(conn, &dn.where_)?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::Node(node),
                    typ: DatabaseMutactionResultType::Delete,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                });
            }
            TopLevelDatabaseMutaction::DeleteNodes(ref dns) => {
                let count = Self::execute_delete_many(conn, Arc::clone(&dns.model), &dns.filter)?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::Count(count),
                    typ: DatabaseMutactionResultType::Many,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                });
            }
            TopLevelDatabaseMutaction::ResetData(ref rd) => {
                Self::execute_reset_data(conn, Arc::clone(&rd.project))?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::None,
                    typ: DatabaseMutactionResultType::Unit,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                });
            }
        };

        Ok(results)
    }

    fn execute_nested(
        conn: &Transaction,
        mutaction: NestedDatabaseMutaction,
        parent_id: GraphqlId,
    ) -> ConnectorResult<DatabaseMutactionResults> {
        let mut results = DatabaseMutactionResults::default();

        match mutaction {
            NestedDatabaseMutaction::CreateNode(ref cn) => {
                let id = Self::execute_nested_create(
                    conn,
                    &parent_id,
                    cn,
                    Arc::clone(&cn.relation_field),
                    &cn.non_list_args,
                    &cn.list_args,
                )?;

                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(id),
                    typ: DatabaseMutactionResultType::Create,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                };

                results.push(result);
            }
            NestedDatabaseMutaction::UpdateNode(ref un) => {
                let id = Self::execute_nested_update(
                    conn,
                    &parent_id,
                    &un.where_,
                    Arc::clone(&un.relation_field),
                    &un.non_list_args,
                    &un.list_args,
                )?;

                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(id),
                    typ: DatabaseMutactionResultType::Update,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                };

                results.push(result);
            }
            NestedDatabaseMutaction::UpsertNode(ref ups) => {
                let ids = Self::get_ids_by_parents(
                    conn,
                    Arc::clone(&ups.relation_field),
                    vec![&parent_id],
                    ups.where_.clone(),
                )?;

                match ids.split_first() {
                    Some(_) => {
                        let id = Self::execute_nested_update(
                            conn,
                            &parent_id,
                            &ups.update.where_,
                            Arc::clone(&ups.update.relation_field),
                            &ups.update.non_list_args,
                            &ups.update.list_args,
                        )?;

                        results.push(DatabaseMutactionResult {
                            identifier: Identifier::Id(id),
                            typ: DatabaseMutactionResultType::Update,
                            mutaction: DatabaseMutaction::Nested(mutaction),
                        });
                    }
                    _ => {
                        let id = Self::execute_nested_create(
                            conn,
                            &parent_id,
                            &ups.create,
                            Arc::clone(&ups.create.relation_field),
                            &ups.create.non_list_args,
                            &ups.create.list_args,
                        )?;

                        results.push(DatabaseMutactionResult {
                            identifier: Identifier::Id(id),
                            typ: DatabaseMutactionResultType::Create,
                            mutaction: DatabaseMutaction::Nested(mutaction),
                        });
                    }
                }
            }
            NestedDatabaseMutaction::Connect(ref connect) => {
                Self::execute_connect(
                    conn,
                    &parent_id,
                    connect,
                    &connect.where_,
                    Arc::clone(&connect.relation_field),
                )?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::None,
                    typ: DatabaseMutactionResultType::Unit,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                });
            }
            NestedDatabaseMutaction::Disconnect(ref disconnect) => {
                Self::execute_disconnect(conn, &parent_id, disconnect, &disconnect.where_)?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::None,
                    typ: DatabaseMutactionResultType::Unit,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                });
            }
            NestedDatabaseMutaction::Set(ref set) => {
                Self::execute_set(conn, &parent_id, set, &set.wheres, Arc::clone(&set.relation_field))?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::None,
                    typ: DatabaseMutactionResultType::Unit,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                });
            }
            NestedDatabaseMutaction::UpdateNodes(ref uns) => {
                let count = Self::execute_nested_update_many(
                    conn,
                    &parent_id,
                    &uns.filter,
                    Arc::clone(&uns.relation_field),
                    &uns.non_list_args,
                    &uns.list_args,
                )?;

                let result = DatabaseMutactionResult {
                    identifier: Identifier::Count(count),
                    typ: DatabaseMutactionResultType::Many,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                };

                results.push(result);
            }
            NestedDatabaseMutaction::DeleteNode(ref dn) => {
                Self::execute_nested_delete(conn, &parent_id, dn, &dn.where_, Arc::clone(&dn.relation_field))?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::None,
                    typ: DatabaseMutactionResultType::Unit,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                });
            }
            NestedDatabaseMutaction::DeleteNodes(ref dns) => {
                let count =
                    Self::execute_nested_delete_many(conn, &parent_id, &dns.filter, Arc::clone(&dns.relation_field))?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::Count(count),
                    typ: DatabaseMutactionResultType::Many,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                })
            }
        }

        Ok(results)
    }

    fn execute_one<T>(conn: &Transaction, query: T) -> ConnectorResult<()>
    where
        T: Into<Query>,
    {
        let (sql, params) = dbg!(visitor::Sqlite::build(query));
        conn.prepare(&sql)?.execute(&params)?;

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
