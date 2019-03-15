use prisma_models::prelude::GraphqlId;
use super::{NodeAddress, CreateNode};

pub struct DatabaseMutactionResults {
    pub results: Vec<DatabaseMutactionResult>,
}

impl DatabaseMutactionResults {
    pub fn new(results: Vec<DatabaseMutactionResult>) -> Self {
        Self { results }
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

    /*
    pub fn id(&self, m: &FurtherNestedMutaction) -> Option<&GraphqlId> {
        self.find(m).map(|mr| mr.id())
    }

    pub fn node_address(&self, m: &FurtherNestedMutaction) -> Option<&GraphqlId> {
        self.find(m).map(|mr| mr.node_address())
    }

    pub fn containts(&self, m: &FurtherNestedMutaction) -> bool {
        self.find(m).is_some()
    }

    fn find(&self, m: &FurtherNestedMutaction) -> Option<&MutactionResult> {
        self.results.iter().find(|mr| mr.mutaction.id() == m.id())
    }
     *
     */
}

pub struct CreateNodeResult {
    pub id: GraphqlId,
    pub mutaction: CreateNode,
    pub node_address: NodeAddress,
}

pub enum DatabaseMutactionResult {
    CreateNode(CreateNodeResult)
}
