use crate::mutaction::*;
use connector::{filter::*, mutaction::*, ConnectorResult};
use prisma_models::*;
use prisma_query::ast::*;
use rusqlite::Transaction;

pub trait DatabaseWrite {
    /// Execute a single statement in the database.
    fn execute_one<T>(conn: &Transaction, query: T) -> ConnectorResult<()>
    where
        T: Into<Query>;

    /// Execute a multiple statements in the database.
    fn execute_many<T>(conn: &Transaction, queries: Vec<T>) -> ConnectorResult<()>
    where
        T: Into<Query>;

    fn update_list_args(
        conn: &Transaction,
        ids: Vec<GraphqlId>,
        model: ModelRef,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<()>;

    fn execute_toplevel(
        conn: &Transaction,
        mutaction: TopLevelDatabaseMutaction,
    ) -> ConnectorResult<DatabaseMutactionResults>;

    fn execute_nested(
        conn: &Transaction,
        mutaction: NestedDatabaseMutaction,
        parent_id: GraphqlId,
    ) -> ConnectorResult<DatabaseMutactionResults>;

    fn execute_create(
        conn: &Transaction,
        model: ModelRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>;

    fn execute_update(
        conn: &Transaction,
        node_selector: &NodeSelector,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>;

    fn execute_update_many(
        conn: &Transaction,
        model: ModelRef,
        filter: &Filter,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<usize>;

    fn execute_delete(conn: &Transaction, mutaction: &DeleteNode) -> ConnectorResult<SingleNode>;
    fn execute_delete_many(conn: &Transaction, mutaction: &DeleteNodes) -> ConnectorResult<usize>;

    fn execute_nested_create(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>;

    fn execute_nested_update(
        conn: &Transaction,
        parent_id: &GraphqlId,
        node_selector: &Option<NodeSelector>,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>;

    fn execute_nested_update_many(
        conn: &Transaction,
        parent_id: &GraphqlId,
        filter: &Option<Filter>,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<usize>;

    fn execute_nested_delete(
        conn: &Transaction,
        parent_id: &GraphqlId,
        mutaction: &NestedDeleteNode,
    ) -> ConnectorResult<()>;

    fn execute_nested_delete_many(
        conn: &Transaction,
        parent_id: &GraphqlId,
        mutaction: &NestedDeleteNodes,
    ) -> ConnectorResult<usize>;

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

    fn execute_reset_data(conn: &Transaction, project: ProjectRef) -> ConnectorResult<()>;
}
