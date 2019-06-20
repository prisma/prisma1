//! Combined mutation executions
mod create_node;
mod delete_node;
mod path;
mod record_address;
mod result;
mod update_node;
mod upsert_node;

pub use create_node::*;
pub use delete_node::*;
pub use path::*;
pub use record_address::*;
pub use result::*;
pub use update_node::*;
pub use upsert_node::*;

use super::filter::{Filter, RecordFinder};
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum DatabaseMutactionResultType {
    Create,
    Update,
    Delete,
    Many,
    Unit,
}

#[derive(Debug, Clone)]
pub enum DatabaseMutaction {
    TopLevel(TopLevelDatabaseMutaction),
    Nested(NestedDatabaseMutaction),
}

#[derive(Debug, Clone)]
pub enum TopLevelDatabaseMutaction {
    CreateNode(CreateNode),
    UpdateNode(UpdateNode),
    DeleteNode(DeleteNode),
    UpsertNode(UpsertNode),
    UpdateNodes(UpdateNodes),
    DeleteNodes(DeleteNodes),
    ResetData(ResetData),
}

#[derive(Debug, Clone)]
pub enum NestedDatabaseMutaction {
    CreateNode(NestedCreateNode),
    UpdateNode(NestedUpdateNode),
    UpsertNode(NestedUpsertNode),
    DeleteNode(NestedDeleteNode),
    Connect(NestedConnect),
    Disconnect(NestedDisconnect),
    Set(NestedSet),
    UpdateNodes(NestedUpdateNodes),
    DeleteNodes(NestedDeleteNodes),
}

#[derive(Default, Debug, Clone)]
pub struct NestedMutactions {
    pub creates: Vec<NestedCreateNode>,
    pub updates: Vec<NestedUpdateNode>,
    pub upserts: Vec<NestedUpsertNode>,
    pub deletes: Vec<NestedDeleteNode>,
    pub connects: Vec<NestedConnect>,
    pub disconnects: Vec<NestedDisconnect>,
    pub sets: Vec<NestedSet>,
    pub update_manys: Vec<NestedUpdateNodes>,
    pub delete_manys: Vec<NestedDeleteNodes>,
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
