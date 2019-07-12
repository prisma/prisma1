use super::*;
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct UpdateRecord {
    pub where_: RecordFinder,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_writes: NestedWriteQueries, // Why is this a struct and not actual queries?
}

#[derive(Debug, Clone)]
pub struct NestedUpdateRecord {
    pub relation_field: Arc<RelationField>,
    pub where_: Option<RecordFinder>,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
    pub nested_writes: NestedWriteQueries,
}

#[derive(Debug, Clone)]
pub struct UpdateManyRecords {
    pub model: ModelRef,
    pub filter: Filter,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
}

#[derive(Debug, Clone)]
pub struct NestedUpdateManyRecords {
    pub relation_field: Arc<RelationField>,
    pub filter: Option<Filter>,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
}
