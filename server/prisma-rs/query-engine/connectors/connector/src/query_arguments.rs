use crate::filter::{Filter, RecordFinder};
use prisma_models::*;

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct SkipAndLimit {
    pub skip: usize,
    pub limit: Option<usize>,
}

#[derive(Debug, Default, Clone)]
pub struct QueryArguments {
    pub skip: Option<i64>,
    pub after: Option<GraphqlId>,
    pub first: Option<i64>,
    pub before: Option<GraphqlId>,
    pub last: Option<i64>,
    pub filter: Option<Filter>,
    pub order_by: Option<OrderBy>,
}

impl QueryArguments {
    pub fn is_with_pagination(&self) -> bool {
        self.last.or(self.first).or(self.skip).is_some()
    }

    pub fn window_limits(&self) -> (i64, i64) {
        let skip = self.skip.unwrap_or(0) + 1;

        match self.last.or(self.first) {
            Some(limited_count) => (skip, limited_count + skip),
            None => (skip, 100000000),
        }
    }

    pub fn skip_and_limit(&self) -> SkipAndLimit {
        match self.last.or(self.first) {
            Some(limited_count) => SkipAndLimit {
                skip: self.skip.unwrap_or(0) as usize,
                limit: Some((limited_count + 1) as usize),
            },
            None => SkipAndLimit {
                skip: self.skip.unwrap_or(0) as usize,
                limit: None,
            },
        }
    }
}

impl From<RecordFinder> for QueryArguments {
    fn from(record_finder: RecordFinder) -> Self {
        QueryArguments::from(Filter::from(record_finder))
    }
}

impl From<Filter> for QueryArguments {
    fn from(filter: Filter) -> Self {
        let mut query_arguments = Self::default();
        query_arguments.filter = Some(filter);
        query_arguments
    }
}
