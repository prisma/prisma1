use super::{DatabaseMutaction, DatabaseMutactionResultType};
use prisma_models::prelude::GraphqlId;

#[derive(Default)]
pub struct DatabaseMutactionResults {
    results: Vec<DatabaseMutactionResult>,
}

#[derive(Clone)]
pub enum IdOrCount {
    Id(GraphqlId),
    Count(usize),
}

#[derive(Clone)]
pub struct DatabaseMutactionResult {
    pub id_or_count: IdOrCount,
    pub typ: DatabaseMutactionResultType,
    pub mutaction: DatabaseMutaction,
}

impl DatabaseMutactionResult {
    pub fn id(&self) -> &GraphqlId {
        match self.id_or_count {
            IdOrCount::Id(ref id) => id,
            _ => panic!("Hey, this doesn't have an id, but a count instead."),
        }
    }

    pub fn count(&self) -> usize {
        match self.id_or_count {
            IdOrCount::Count(count) => count,
            _ => panic!("Hey, this doesn't have a count, but an id instead."),
        }
    }
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
