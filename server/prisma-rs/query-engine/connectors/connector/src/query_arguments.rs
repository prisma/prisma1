use crate::{
    filter::{Filter, ScalarCondition, ScalarFilter},
    node_selector::NodeSelector,
};
use prisma_models::prelude::*;

#[derive(Debug, Default, Clone)]
pub struct QueryArguments {
    pub skip: Option<u32>,
    pub after: Option<GraphqlId>,
    pub first: Option<u32>,
    pub before: Option<GraphqlId>,
    pub last: Option<u32>,
    pub filter: Option<Filter>,
    pub order_by: Option<OrderBy>,
}

impl QueryArguments {
    pub fn is_with_pagination(&self) -> bool {
        self.last.or(self.first).or(self.skip).is_some()
    }

    pub fn window_limits(&self) -> (u32, u32) {
        let skip = self.skip.unwrap_or(0) + 1;

        match self.last.or(self.first) {
            Some(limited_count) => (skip, limited_count + skip),
            None => (skip, 100000000),
        }
    }
}

impl From<NodeSelector> for QueryArguments {
    fn from(node_selector: NodeSelector) -> Self {
        let filter = Filter::Scalar(ScalarFilter {
            field: node_selector.field,
            condition: ScalarCondition::Equals(node_selector.value),
        });

        QueryArguments::from(filter)
    }
}

impl From<Filter> for QueryArguments {
    fn from(filter: Filter) -> Self {
        let mut query_arguments = Self::default();
        query_arguments.filter = Some(filter);
        query_arguments
    }
}
