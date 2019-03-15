use parking_lot::RwLock;
use std::sync::Arc;
use prisma_models::prelude::GraphqlId;
use prisma_query::ast::Query;
use connector::{DatabaseMutaction};

type ReturnSwitch = Arc<RwLock<Returning>>;

#[derive(Debug, Clone)]
pub enum Returning {
    Expected,
    Got(GraphqlId),
}

impl Returning {
    pub fn set(&mut self, id: Option<GraphqlId>) {
        if let Some(id) = id {
            *self = Returning::Got(id);
        }
    }
}

#[derive(Debug, Clone)]
pub struct MutactionStep {
    pub query: Query,
    pub returning: Option<ReturnSwitch>,
    pub needing: Option<ReturnSwitch>,
}

#[derive(Debug, Clone)]
pub struct MutactionPlan {
    pub steps: Vec<MutactionStep>,
    pub mutaction: DatabaseMutaction,
}
