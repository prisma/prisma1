use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct DeleteRecord {
    pub where_: RecordFinder,
}

#[derive(Debug, Clone)]
pub struct NestedDeleteRecord {
    pub relation_field: Arc<RelationField>,
    pub where_: Option<RecordFinder>,
}

#[derive(Debug, Clone)]
pub struct DeleteManyRecords {
    pub model: ModelRef,
    pub filter: Filter,
}

#[derive(Debug, Clone)]
pub struct NestedDeleteManyRecords {
    pub relation_field: Arc<RelationField>,
    pub filter: Option<Filter>,
}
