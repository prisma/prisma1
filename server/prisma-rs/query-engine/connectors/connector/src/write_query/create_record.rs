use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct CreateRecord {
    pub model: ModelRef,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_writes: NestedWriteQueries,
}

#[derive(Debug, Clone)]
pub struct NestedCreateRecord {
    pub relation_field: Arc<RelationField>,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub top_is_create: bool,
    pub nested_writes: NestedWriteQueries,
}

impl From<CreateRecord> for WriteQuery {
    fn from(cn: CreateRecord) -> WriteQuery {
        WriteQuery::Root(RootWriteQuery::CreateRecord(cn))
    }
}
