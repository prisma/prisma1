use super::DatabaseMutactionResultType;
use prisma_models::prelude::{GraphqlId, SingleRecord};

#[derive(Debug, Clone)]
pub enum Identifier {
    Id(GraphqlId),
    Count(usize),
    Record(SingleRecord),
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

    pub fn record(&self) -> &SingleRecord {
        match self.identifier {
            Identifier::Record(ref record) => record,
            _ => panic!("No record defined in DatabaseMutactionResult"),
        }
    }
}
