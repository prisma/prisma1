use crate::{mutaction::NestedActions, DatabaseRead, DatabaseWrite, Sqlite};
use connector::*;
use prisma_models::*;
use prisma_query::ast::*;
use serde_json::Value;
use std::sync::Arc;

impl DatabaseMutactionExecutor for Sqlite {
    fn execute_raw(&self, _query: String) -> ConnectorResult<Value> {
        // self.sqlite.with_connection(&db_name, |conn| {
        //     let res = conn
        //         .prepare(&query)?
        //         .query_map(&params, |row| f(row))?
        //         .map(|row_res| row_res.unwrap())
        //         .collect();

        //     Ok(res)
        // });
        Ok(Value::String("hello world!".to_string()))
    }

    fn execute_create(&self, db_name: String, mutaction: &CreateNode) -> ConnectorResult<GraphqlId> {
        self.with_transaction(&db_name, |conn| {
            Self::create_node(
                conn,
                mutaction.model.clone(),
                mutaction.non_list_args.clone(),
                mutaction.list_args.clone(),
            )
        })
    }

    fn execute_update(&self, db_name: String, mutaction: &UpdateNode) -> ConnectorResult<GraphqlId> {
        self.with_transaction(&db_name, |conn| {
            let model = mutaction.where_.field.model();
            let id = Self::id_for(conn, Arc::clone(&model), &mutaction.where_)?;
            Self::update_node(conn, id, mutaction)
        })
    }

    fn execute_update_many(&self, db_name: String, mutaction: &UpdateNodes) -> ConnectorResult<usize> {
        self.with_transaction(&db_name, |conn| {
            let ids = dbg!(Self::ids_for(conn, mutaction.model.clone(), mutaction.filter.clone())?);
            Self::update_nodes(conn, ids, mutaction)
        })
    }

    fn execute_upsert(
        &self,
        db_name: String,
        mutaction: &UpsertNode,
    ) -> ConnectorResult<(GraphqlId, DatabaseMutactionResultType)> {
        self.with_transaction(&db_name, |conn| {
            let model = mutaction.where_.field.model();

            match Self::id_for(conn, Arc::clone(&model), &mutaction.where_) {
                Err(_e @ ConnectorError::NodeNotFoundForWhere { .. }) => {
                    let m = &mutaction.create;
                    let id = Self::create_node(conn, m.model.clone(), m.non_list_args.clone(), m.list_args.clone())?;

                    Ok((id, DatabaseMutactionResultType::Create))
                }
                Ok(id) => {
                    let id = Self::update_node(conn, id.clone(), &mutaction.update)?;

                    Ok((id, DatabaseMutactionResultType::Update))
                }
                Err(e) => Err(e),
            }
        })
    }

    fn execute_nested_create(
        &self,
        db_name: String,
        parent_id: &GraphqlId,
        mutaction: &NestedCreateNode,
    ) -> ConnectorResult<GraphqlId> {
        self.with_transaction(&db_name, |conn| {
            let relation = mutaction.relation_field.relation();

            if let Some(Query::Select(select)) = mutaction.required_check(parent_id)? {
                let ids = Self::query(conn, select, |row| {
                    let id: GraphqlId = row.get(0);
                    Ok(id)
                })?;

                if ids.into_iter().next().is_some() {
                    return Err(ConnectorError::RelationViolation {
                        relation_name: relation.name.clone(),
                        model_a_name: relation.model_a().name.clone(),
                        model_b_name: relation.model_b().name.clone(),
                    });
                }
            };

            if let Some(query) = mutaction.removal_action(parent_id) {
                Self::execute_one(conn, query)?;
            }

            Self::create_node_and_connect_to_parent(conn, parent_id, mutaction)
        })
    }

    fn execute_nested_update(
        &self,
        db_name: String,
        parent_id: &GraphqlId,
        mutaction: &NestedUpdateNode,
    ) -> ConnectorResult<GraphqlId> {
        self.with_transaction(&db_name, |conn| {
            if let Some(ref node_selector) = mutaction.where_ {
                let ids = Self::ids_for(conn, node_selector.field.model(), node_selector.clone());

                if ids.into_iter().next().is_none() {
                    return Err(ConnectorError::NodeNotFoundForWhere(NodeSelectorInfo::from(
                        node_selector,
                    )));
                }
            }

            let ids = Self::get_ids_by_parents(
                conn,
                Arc::clone(&mutaction.relation_field),
                vec![parent_id.clone()],
                &mutaction.where_,
            )?;

            match ids.into_iter().next() {
                Some(id) => Self::update_node(conn, id.clone(), mutaction),
                None => Err(ConnectorError::NodesNotConnected {
                    relation_name: mutaction.relation_field.relation().name.clone(),
                    parent_name: mutaction.relation_field.model().name.clone(),
                    parent_where: None,
                    child_name: mutaction.relation_field.related_model().name.clone(),
                    child_where: mutaction.where_.as_ref().map(NodeSelectorInfo::from),
                }),
            }
        })
    }

    fn execute_delete(&self, _db_name: String, _mutaction: &DeleteNode) -> ConnectorResult<SingleNode> {
        unimplemented!()
    }
}
