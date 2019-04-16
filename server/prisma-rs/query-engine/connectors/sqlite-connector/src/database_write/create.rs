use crate::mutaction::NestedActions;
use connector::ConnectorResult;
use prisma_models::{GraphqlId, ModelRef, PrismaArgs, PrismaListValue, RelationFieldRef};
use rusqlite::Transaction;

pub trait DatabaseCreate {
    fn execute_create(
        conn: &Transaction,
        model: ModelRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>;

    fn execute_nested_create(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(String, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>;
}
