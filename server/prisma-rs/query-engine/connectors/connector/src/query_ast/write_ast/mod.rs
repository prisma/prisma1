//! Combined write query executions

mod create_record;
mod delete_record;
mod path;
mod record_address;
mod update_record;
mod upsert_record;

pub use create_record::*;
pub use delete_record::*;
pub use path::*;
pub use record_address::*;
pub use update_record::*;
pub use upsert_record::*;

use crate::filter::{Filter, RecordFinder};
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub enum WriteQuery {
    Root(RootWriteQuery),
    Nested(NestedWriteQuery),
}

#[derive(Debug, Clone)]
pub enum RootWriteQuery {
    CreateRecord(CreateRecord),
    UpdateRecord(UpdateRecord),
    DeleteRecord(DeleteRecord),
    UpsertRecord(UpsertRecord),
    UpdateManyRecords(UpdateManyRecords),
    DeleteManyRecords(DeleteManyRecords),
    ResetData(ResetData),
}

#[derive(Debug, Clone)]
pub enum NestedWriteQuery {
    CreateRecord(NestedCreateRecord),
    UpdateRecord(NestedUpdateRecord),
    UpsertRecord(NestedUpsertRecord),
    DeleteRecord(NestedDeleteRecord),
    Connect(NestedConnect),
    Disconnect(NestedDisconnect),
    Set(NestedSet),
    UpdateManyRecords(NestedUpdateManyRecords),
    DeleteManyRecords(NestedDeleteManyRecords),
}

#[derive(Default, Debug, Clone)]
pub struct NestedWriteQueries {
    pub creates: Vec<NestedCreateRecord>,
    pub updates: Vec<NestedUpdateRecord>,
    pub upserts: Vec<NestedUpsertRecord>,
    pub deletes: Vec<NestedDeleteRecord>,
    pub connects: Vec<NestedConnect>,
    pub disconnects: Vec<NestedDisconnect>,
    pub sets: Vec<NestedSet>,
    pub update_manys: Vec<NestedUpdateManyRecords>,
    pub delete_manys: Vec<NestedDeleteManyRecords>,
}

// SET

#[derive(Debug, Clone)]
pub struct NestedSet {
    pub relation_field: Arc<RelationField>,
    pub wheres: Vec<RecordFinder>,
}

// CONNECT

#[derive(Debug, Clone)]
pub struct NestedConnect {
    pub relation_field: RelationFieldRef,
    pub where_: RecordFinder,
    pub top_is_create: bool,
}

// DISCONNECT

#[derive(Debug, Clone)]
pub struct NestedDisconnect {
    pub relation_field: Arc<RelationField>,
    pub where_: Option<RecordFinder>,
}

#[derive(Debug, Clone)]
pub struct ResetData {
    pub internal_data_model: InternalDataModelRef,
}
