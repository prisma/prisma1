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
                let node = Self::execute_delete(conn, &dn)?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::Node(node),
                    typ: DatabaseMutactionResultType::Delete,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                });
            }
            TopLevelDatabaseMutaction::DeleteNodes(ref dns) => {
                let count = Self::execute_delete_many(conn, dns)?;

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
                Self::execute_nested_delete(conn, &parent_id, dn)?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::None,
                    typ: DatabaseMutactionResultType::Unit,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                });
            }
            NestedDatabaseMutaction::DeleteNodes(ref dns) => {
                let count = Self::execute_nested_delete_many(conn, &parent_id, dns)?;

                results.push(DatabaseMutactionResult {
                    identifier: Identifier::Count(count),
                    typ: DatabaseMutactionResultType::Many,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                })
            }
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
        let updating = MutationBuilder::update_one(Arc::clone(&model), &id, non_list_args)?;

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

        let updates = {
            let ids: Vec<&GraphqlId> = ids.iter().map(|id| &*id).collect();
            MutationBuilder::update_many(Arc::clone(&model), ids.as_slice(), non_list_args)?
        };

        Self::execute_many(conn, updates)?;
        Self::update_list_args(conn, ids, Arc::clone(&model), list_args.to_vec())?;

        Ok(count)
    }

    fn execute_delete(conn: &Transaction, mutaction: &DeleteNode) -> ConnectorResult<SingleNode> {
        let model = mutaction.where_.field.model();
        let node = Self::find_node(conn, &mutaction.where_)?;

        let id = node.get_id_value(Arc::clone(&model)).unwrap();
        let deletes = MutationBuilder::delete_many(model, &[id]);

        mutaction.check_relation_violations(&[id], |select| {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            Ok(ids.into_iter().next())
        })?;

        Self::execute_many(conn, deletes)?;

        Ok(node)
    }

    fn execute_delete_many(conn: &Transaction, mutaction: &DeleteNodes) -> ConnectorResult<usize> {
        let ids = Self::ids_for(conn, Arc::clone(&mutaction.model), mutaction.filter.clone())?;
        let ids: Vec<&GraphqlId> = ids.iter().map(|id| &*id).collect();
        let count = ids.len();

        mutaction.check_relation_violations(ids.as_slice(), |select| {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            Ok(ids.into_iter().next())
        })?;

        let deletes = { MutationBuilder::delete_many(Arc::clone(&mutaction.model), ids.as_slice()) };

        Self::execute_many(conn, deletes)?;

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
        if let Some((select, check)) = actions.required_check(parent_id)? {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            check.call_box(ids.into_iter().next())?
        };

        if let Some(query) = actions.parent_removal(parent_id) {
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
            Self::id_for(conn, node_selector)?;
        };

        let id = Self::get_id_by_parent(conn, Arc::clone(&relation_field), parent_id, node_selector)?;

        let node_selector = NodeSelector::from((relation_field.related_model().fields().id(), id));
        Self::execute_update(conn, &node_selector, non_list_args, list_args)
    }

    fn execute_nested_update_many(
        conn: &Transaction,
        parent_id: &GraphqlId,
        filter: &Option<Filter>,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<usize> {
        let ids = Self::get_ids_by_parents(conn, Arc::clone(&relation_field), vec![parent_id], filter.clone())?;
        let count = ids.len();

        let updates = {
            let ids: Vec<&GraphqlId> = ids.iter().map(|id| &*id).collect();
            MutationBuilder::update_many(relation_field.related_model(), ids.as_slice(), non_list_args)?
        };

        Self::execute_many(conn, updates)?;
        Self::update_list_args(conn, ids, relation_field.model(), list_args.to_vec())?;

        Ok(count)
    }

    fn execute_nested_delete(
        conn: &Transaction,
        parent_id: &GraphqlId,
        mutaction: &NestedDeleteNode,
    ) -> ConnectorResult<()> {
        if let Some(ref node_selector) = mutaction.where_ {
            Self::id_for(conn, node_selector)?;
        };

        let child_id = Self::get_id_by_parent(
            conn,
            Arc::clone(&mutaction.relation_field),
            parent_id,
            &mutaction.where_,
        )
        .map_err(|e| match e {
            ConnectorError::NodesNotConnected {
                relation_name,
                parent_name,
                parent_where: _,
                child_name,
                child_where,
            } => {
                let model = mutaction.relation_field.model().clone();

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
            let (select, check) = mutaction.ensure_connected(parent_id, &child_id);
            let ids = Self::query(conn, select, Self::fetch_id)?;
            check.call_box(ids.into_iter().next())?;
        }

        mutaction.check_relation_violations(&[&child_id; 1], |select| {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            Ok(ids.into_iter().next())
        })?;

        let deletes = MutationBuilder::delete_many(mutaction.relation_field.related_model(), &[&child_id]);
        Self::execute_many(conn, deletes)?;

        Ok(())
    }

    fn execute_nested_delete_many(
        conn: &Transaction,
        parent_id: &GraphqlId,
        mutaction: &NestedDeleteNodes,
    ) -> ConnectorResult<usize> {
        let ids = Self::get_ids_by_parents(
            conn,
            Arc::clone(&mutaction.relation_field),
            vec![parent_id],
            mutaction.filter.clone(),
        )?;

        let count = ids.len();
        let ids: Vec<&GraphqlId> = ids.iter().map(|id| &*id).collect();

        mutaction.check_relation_violations(ids.as_slice(), |select| {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            Ok(ids.into_iter().next())
        })?;

        let deletes = MutationBuilder::delete_many(mutaction.relation_field.related_model(), ids.as_slice());

        Self::execute_many(conn, deletes)?;

        Ok(count)
    }

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
            let (deletes, inserts) = MutationBuilder::update_scalar_list_values(&table, &list_value, ids.to_vec());

            Self::execute_many(conn, deletes)?;
            Self::execute_many(conn, inserts)?;
        }

        Ok(())
    }

    fn execute_reset_data(conn: &Transaction, project: ProjectRef) -> ConnectorResult<()> {
        Self::without_foreign_key_checks(conn, || {
            let deletes = MutationBuilder::truncate_tables(project);

            Self::execute_many(conn, deletes)
        })?;

        Ok(())
    }
}
