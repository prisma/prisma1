use crate::{filter::NodeSelector, query_arguments::QueryArguments, ConnectorResult};
use prisma_models::prelude::*;
use prisma_models::ScalarFieldRef;

/// Methods for fetching data.
pub trait DataResolver {
    /// Find one record.
    fn get_node_by_where(
        &self,
        node_selector: &NodeSelector,
        selected_fields: &SelectedFields,
    ) -> ConnectorResult<Option<SingleNode>>;

    /// Filter many records.
    fn get_nodes(
        &self,
        model: ModelRef,
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> ConnectorResult<ManyNodes>;

    /// Filter records related to the parent.
    fn get_related_nodes(
        &self,
        from_field: RelationFieldRef,
        from_node_ids: &[GraphqlId],
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> ConnectorResult<ManyNodes>;

    /// Fetch scalar list values for the parent.
    fn get_scalar_list_values_by_node_ids(
        &self,
        list_field: ScalarFieldRef,
        node_ids: Vec<GraphqlId>,
    ) -> ConnectorResult<Vec<ScalarListValues>>;

    /// Count the items in the model with the given arguments.
    fn count_by_model(&self, model: ModelRef, query_arguments: QueryArguments) -> ConnectorResult<usize>;

    /// Count the items in the table.
    fn count_by_table(&self, database: &str, table: &str) -> ConnectorResult<usize>;
}

#[derive(Debug)]
pub struct ScalarListValues {
    pub node_id: GraphqlId,
    pub values: Vec<PrismaValue>,
}
