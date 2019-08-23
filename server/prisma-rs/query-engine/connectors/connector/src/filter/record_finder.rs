use prisma_models::prelude::*;
use std::sync::Arc;

/// Designates a specific record to find by a field and a value that field should have.
#[derive(Debug, Clone)]
pub struct RecordFinder {
    pub field: Arc<ScalarField>,
    pub value: PrismaValue,
}

impl<T> From<(Arc<ScalarField>, T)> for RecordFinder
where
    T: Into<PrismaValue>,
{
    fn from(tup: (Arc<ScalarField>, T)) -> RecordFinder {
        RecordFinder {
            field: tup.0,
            value: tup.1.into(),
        }
    }
}

impl RecordFinder {
    pub fn new<T>(field: Arc<ScalarField>, value: T) -> Self
    where
        T: Into<PrismaValue>,
    {
        Self {
            field,
            value: value.into(),
        }
    }
}
