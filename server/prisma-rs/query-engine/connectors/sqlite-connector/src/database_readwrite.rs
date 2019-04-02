use crate::SelectDefinition;
use connector::*;
use prisma_models::*;
use prisma_query::ast::*;
use rusqlite::{Row, Transaction};

pub trait DatabaseRead {
    /// Execute the `SELECT` and return a vector mapped with `F`.
    ///
    /// See implementations for usage.
    fn query<F, T, S>(conn: &Transaction, query: S, f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
        S: Into<Select>;

    /// Count the records of the given query.
    fn count<C, T>(conn: &Transaction, table: T, into_args: C) -> ConnectorResult<usize>
    where
        C: Into<ConditionTree>,
        T: Into<Table>;

    /// Find all ids from the `Model` with the filter being true.
    ///
    /// See implementations for usage.
    fn ids_for<T>(conn: &Transaction, model: ModelRef, into_args: T) -> ConnectorResult<Vec<GraphqlId>>
    where
        T: SelectDefinition;

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
