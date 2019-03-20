use super::{DatabaseMutaction, DatabaseMutactionResultType};
use prisma_models::prelude::GraphqlId;

#[derive(Default)]
pub struct DatabaseMutactionResults {
    results: Vec<DatabaseMutactionResult>,
}

#[derive(Clone)]
pub struct DatabaseMutactionResult {
    pub id: GraphqlId,
    pub typ: DatabaseMutactionResultType,
    pub mutaction: DatabaseMutaction,
}

impl DatabaseMutactionResults {
    pub fn push(&mut self, result: DatabaseMutactionResult) {
        self.results.push(result);
    }

    pub fn pop(&mut self) -> Option<DatabaseMutactionResult> {
        self.results.pop()
    }

    pub fn merge(&mut self, mut other_result: DatabaseMutactionResults) {
        for mr in other_result.results.drain(0..) {
            self.results.push(mr);
        }
    }

    pub fn merge_all(&mut self, mut other_results: Vec<DatabaseMutactionResults>) {
        for mrs in other_results.drain(0..) {
            self.merge(mrs);
        }
    }
}
