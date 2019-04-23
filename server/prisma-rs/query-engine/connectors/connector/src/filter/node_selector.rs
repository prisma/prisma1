use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct NodeSelector {
    pub field: Arc<ScalarField>,
    pub value: PrismaValue,
}

impl<T> From<(Arc<ScalarField>, T)> for NodeSelector
where
    T: Into<PrismaValue>,
{
    fn from(tup: (Arc<ScalarField>, T)) -> NodeSelector {
        NodeSelector {
            field: tup.0,
            value: tup.1.into(),
        }
    }
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
