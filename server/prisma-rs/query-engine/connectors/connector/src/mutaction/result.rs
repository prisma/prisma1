use super::DatabaseMutactionResultType;
use prisma_models::prelude::{GraphqlId, SingleNode};

#[derive(Debug, Clone)]
pub enum Identifier {
    Id(GraphqlId),
    Count(usize),
    Node(SingleNode),
    None,
}

#[derive(Debug, Clone)]
pub struct DatabaseMutactionResult {
    pub identifier: Identifier,
    pub typ: DatabaseMutactionResultType,
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
