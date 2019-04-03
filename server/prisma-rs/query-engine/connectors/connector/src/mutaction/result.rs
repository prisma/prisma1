use super::{DatabaseMutaction, DatabaseMutactionResultType};
use prisma_models::prelude::{GraphqlId, SingleNode};

#[derive(Default)]
pub struct DatabaseMutactionResults {
    results: Vec<DatabaseMutactionResult>,
}

#[derive(Clone)]
pub enum Identifier {
    Id(GraphqlId),
    Count(usize),
    Node(SingleNode),
    None,
}

#[derive(Clone)]
pub struct DatabaseMutactionResult {
    pub identifier: Identifier,
    pub typ: DatabaseMutactionResultType,
    pub mutaction: DatabaseMutaction,
}

impl DatabaseMutactionResult {
    pub fn id(&self) -> &GraphqlId {
        match self.identifier {
            Identifier::Id(ref id) => id,
            _ => panic!("No id defined in DatabaseMutactionResult"),
        }
    }

    pub fn count(&self) -> usize {
        match self.identifier {
            Identifier::Count(count) => count,
            _ => panic!("No count defined in DatabaseMutactionResult"),
        }
    }

    pub fn node(&self) -> &SingleNode {
        match self.identifier {
            Identifier::Node(ref node) => node,
            _ => panic!("No node defined in DatabaseMutactionResult"),
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
