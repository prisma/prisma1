use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug)]
pub struct NodeSelector {
    pub field: Arc<ScalarField>,
    pub value: PrismaValue,
}
