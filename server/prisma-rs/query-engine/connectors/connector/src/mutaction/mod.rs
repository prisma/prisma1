mod builder;
mod create_node;
mod delete_node;
mod node_address;
mod path;
mod relay_id;
mod result;
mod update_node;
mod upsert_node;

pub use builder::*;
pub use create_node::*;
pub use delete_node::*;
pub use node_address::*;
pub use path::*;
pub use relay_id::*;
pub use result::*;
pub use update_node::*;
pub use upsert_node::*;

use super::{Filter, NodeSelector};
use prisma_models::prelude::*;
use std::sync::Arc;

pub trait NestedMutaction {
    fn nested_mutactions(&self) -> &[&DatabaseMutaction];
}

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

#[derive(Debug, Default, Clone)]
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

#[derive(Debug, Clone)]
pub struct NestedConnect {
    pub relation_field: Arc<RelationField>,
    pub where_: NodeSelector,
    pub top_is_create: bool,
}

// SET

#[derive(Debug, Clone)]
pub struct NestedSet {
    pub relation_field: Arc<RelationField>,
    pub wheres: Vec<NodeSelector>,
}

// DISCONNECT

#[derive(Debug, Clone)]
pub struct NestedDisconnect {
    pub relation_field: Arc<RelationField>,
    pub where_: Option<NodeSelector>,
}

#[derive(Debug, Clone)]
pub struct ResetData {
    pub project: ProjectRef,
}
