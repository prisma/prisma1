use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct NodeSelector {
    pub field: Arc<ScalarField>,
    pub value: PrismaValue,
}
