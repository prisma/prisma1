use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct NodeSelector {
    pub field: Arc<ScalarField>,
    pub value: PrismaValue,
}

impl NodeSelector {
    pub fn new<T>(field: Arc<ScalarField>, value: T) -> Self
    where
        T: Into<PrismaValue>,
    {
        Self {
            field: field,
            value: value.into(),
        }
    }
}
