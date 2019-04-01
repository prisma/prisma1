use connector::*;
use prisma_models::*;
use prisma_query::ast::*;
use rusqlite::{Row, Transaction};

pub trait DatabaseRead {
    /// Execute the `SELECT` and return a vector mapped with `F`.
    ///
    /// See implementations for usage.
    fn query<F, T>(conn: &Transaction, query: Select, f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>;

    /// Find all ids from the `Model` with the filter being true.
    ///
    /// See implementations for usage.
    fn ids_for<T>(conn: &Transaction, model: ModelRef, into_args: T) -> ConnectorResult<Vec<GraphqlId>>
    where
        T: Into<QueryArguments>;

    /// Find all ids from the `Model` with the filter being true.
    ///
    /// See implementations for usage.
    fn id_for(conn: &Transaction, model: ModelRef, node_selector: &NodeSelector) -> ConnectorResult<GraphqlId>;

    fn get_ids_by_parents(
        conn: &Transaction,
        parent_field: RelationFieldRef,
        parent_id: Vec<GraphqlId>,
        selector: &Option<NodeSelector>,
    ) -> ConnectorResult<Vec<GraphqlId>>;
}

pub trait DatabaseWrite {
    fn execute_one<T>(conn: &Transaction, query: T) -> ConnectorResult<()>
    where
        T: Into<Query>;

    fn execute_many<T>(conn: &Transaction, queries: Vec<T>) -> ConnectorResult<()>
    where
        T: Into<Query>;

    fn create_node(
        conn: &Transaction,
        model: ModelRef,
        non_list_args: PrismaArgs,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<GraphqlId>;

    fn create_list_args(
        conn: &Transaction,
        id: &GraphqlId,
        model: ModelRef,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<()>;

    fn create_node_and_connect_to_parent(
        conn: &Transaction,
        parent_id: &GraphqlId,
        mutaction: &NestedCreateNode,
    ) -> ConnectorResult<GraphqlId>;

    fn update_list_args(
        conn: &Transaction,
        ids: Vec<GraphqlId>,
        model: ModelRef,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<()>;

    fn update_node<T>(conn: &Transaction, id: GraphqlId, mutaction: &T) -> ConnectorResult<GraphqlId>
    where
        T: SharedUpdateLogic;

    fn update_nodes(conn: &Transaction, ids: Vec<GraphqlId>, mutaction: &UpdateNodes) -> ConnectorResult<usize>;
}
