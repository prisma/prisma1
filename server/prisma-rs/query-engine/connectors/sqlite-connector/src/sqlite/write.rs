use crate::{mutaction::*, DatabaseRead, DatabaseWrite, Sqlite};
use connector::{error::*, filter::*, mutaction::*, ConnectorResult};
use prisma_models::*;
use prisma_query::{
    ast::*,
    visitor::{self, *},
};
use rusqlite::Transaction;
use std::sync::Arc;

impl DatabaseWrite for Sqlite {
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

    fn update_list_args(
        conn: &Transaction,
        ids: Vec<GraphqlId>,
        model: ModelRef,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<()> {
        for (field_name, list_value) in list_args {
            let field = model.fields().find_from_scalar(&field_name).unwrap();
            let table = field.scalar_list_table();
            let (deletes, inserts) = MutationBuilder::update_scalar_list_value_by_ids(table, &list_value, ids.to_vec());

            Self::execute_many(conn, deletes)?;
            Self::execute_many(conn, inserts)?;
        }

        Ok(())
    }

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
            TopLevelDatabaseMutaction::DeleteNode(_) => unimplemented!(),
            TopLevelDatabaseMutaction::DeleteNodes(_) => unimplemented!(),
            TopLevelDatabaseMutaction::ResetData(_) => unimplemented!(),
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
                let ids =
                    Self::get_ids_by_parents(conn, Arc::clone(&ups.relation_field), vec![&parent_id], &ups.where_)?;

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
            NestedDatabaseMutaction::DeleteNode(_) => unimplemented!(),
            NestedDatabaseMutaction::Connect(_) => unimplemented!(),
            NestedDatabaseMutaction::Disconnect(_) => unimplemented!(),
            NestedDatabaseMutaction::Set(_) => unimplemented!(),
            NestedDatabaseMutaction::UpdateNodes(_) => unimplemented!(),
            NestedDatabaseMutaction::DeleteNodes(_) => unimplemented!(),
        }

        Ok(results)
    }

    fn execute_create(
        conn: &Transaction,
        model: ModelRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId> {
        let (insert, returned_id) = MutationBuilder::create_node(Arc::clone(&model), non_list_args.clone());

        Self::execute_one(conn, insert)?;

        let id = match returned_id {
            Some(id) => id,
            None => GraphqlId::Int(conn.last_insert_rowid() as usize),
        };

        for (field_name, list_value) in list_args.to_vec() {
            let field = model.fields().find_from_scalar(&field_name).unwrap();
            let table = field.scalar_list_table();

            if let Some(insert) = MutationBuilder::create_scalar_list_value(table.table(), &list_value, &id) {
                Self::execute_one(conn, insert)?;
            }
        }

        Ok(id)
    }

    fn execute_update(
        conn: &Transaction,
        node_selector: &NodeSelector,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId> {
        let model = node_selector.field.model();
        let id = Self::id_for(conn, node_selector)?;
        let updating = MutationBuilder::update_by_id(Arc::clone(&model), id.clone(), non_list_args)?;

        if let Some(update) = updating {
            Self::execute_one(conn, update)?;
        }

        Self::update_list_args(conn, vec![id.clone()], Arc::clone(&model), list_args.to_vec())?;

        Ok(id)
    }

    fn execute_update_many(
        conn: &Transaction,
        model: ModelRef,
        filter: &Filter,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<usize> {
        let ids = Self::ids_for(conn, Arc::clone(&model), filter.clone())?;
        let count = ids.len();

        let updates = MutationBuilder::update_by_ids(Arc::clone(&model), non_list_args, ids.clone())?;

        Self::execute_many(conn, updates)?;
        Self::update_list_args(conn, ids, Arc::clone(&model), list_args.to_vec())?;

        Ok(count)
    }

    fn execute_nested_create(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId> {
        if let Some(Query::Select(select)) = actions.required_check(parent_id)? {
            let ids = Self::query(conn, select, |row| {
                let id: GraphqlId = row.get(0);
                Ok(id)
            })?;

            if ids.into_iter().next().is_some() {
                let relation = relation_field.relation();

                return Err(ConnectorError::RelationViolation {
                    relation_name: relation.name.clone(),
                    model_a_name: relation.model_a().name.clone(),
                    model_b_name: relation.model_b().name.clone(),
                });
            }
        };

        if let Some(query) = actions.removal_action(parent_id) {
            Self::execute_one(conn, query)?;
        }

        let related_field = relation_field.related_field();

        if related_field.relation_is_inlined_in_parent() {
            let mut prisma_args = non_list_args.clone();
            prisma_args.insert(related_field.name.clone(), parent_id.clone());

            Self::execute_create(conn, relation_field.related_model(), &prisma_args, list_args)
        } else {
            let id = Self::execute_create(conn, relation_field.related_model(), non_list_args, list_args)?;
            let relation_query = MutationBuilder::create_relation(relation_field, parent_id, &id);

            Self::execute_one(conn, relation_query)?;
            Ok(id)
        }
    }

    fn execute_nested_update(
        conn: &Transaction,
        parent_id: &GraphqlId,
        node_selector: &Option<NodeSelector>,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId> {
        if let Some(ref node_selector) = node_selector {
            let ids = Self::ids_for(conn, node_selector.field.model(), node_selector.clone());

            if ids.into_iter().next().is_none() {
                return Err(ConnectorError::NodeNotFoundForWhere(NodeSelectorInfo::from(
                    node_selector,
                )));
            }
        };

        let ids = Self::get_ids_by_parents(conn, Arc::clone(&relation_field), vec![parent_id], &node_selector)?;

        match ids.into_iter().next() {
            Some(id) => {
                let node_selector = NodeSelector::from((relation_field.related_model().fields().id(), id));

                Self::execute_update(conn, &node_selector, non_list_args, list_args)
            }
            None => Err(ConnectorError::NodesNotConnected {
                relation_name: relation_field.relation().name.clone(),
                parent_name: relation_field.model().name.clone(),
                parent_where: None,
                child_name: relation_field.related_model().name.clone(),
                child_where: node_selector.as_ref().map(NodeSelectorInfo::from),
            }),
        }
    }
}
