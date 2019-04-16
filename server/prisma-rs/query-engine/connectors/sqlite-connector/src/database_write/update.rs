use connector::{
    filter::{Filter, NodeSelector},
    ConnectorResult,
};
use prisma_models::*;
use rusqlite::Transaction;

pub trait DatabaseUpdate {
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

    fn update_list_args(
        conn: &Transaction,
        ids: Vec<GraphqlId>,
        model: ModelRef,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<()>;
}
