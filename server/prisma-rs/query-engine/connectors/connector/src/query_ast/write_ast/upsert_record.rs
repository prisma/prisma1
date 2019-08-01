use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct UpsertRecord {
    pub where_: RecordFinder,
    pub create: CreateRecord,
    pub update: UpdateRecord,
}

#[derive(Debug, Clone)]
pub struct NestedUpsertRecord {
    pub relation_field: Arc<RelationField>,
    pub where_: Option<RecordFinder>,
    pub create: NestedCreateRecord,
    pub update: NestedUpdateRecord,
}

impl From<UpsertRecord> for WriteQuery {
    fn from(u: UpsertRecord) -> WriteQuery {
        WriteQuery::Root(RootWriteQuery::UpsertRecord(u))
    }
}