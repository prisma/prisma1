use crate::filter::Filter;
use prisma_models::prelude::*;

#[derive(Debug, Default)]
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
