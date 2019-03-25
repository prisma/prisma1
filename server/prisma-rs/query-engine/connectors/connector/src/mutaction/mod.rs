mod builder;
mod create_node;
mod node_address;
mod path;
mod relay_id;
mod result;
mod update_node;

pub use builder::*;
pub use create_node::*;
pub use node_address::*;
pub use path::*;
pub use relay_id::*;
pub use result::*;
pub use update_node::*;

use super::{Filter, NodeSelector};
use prisma_models::prelude::*;

pub trait NestedMutaction {
    fn nested_mutactions(&self) -> &[&DatabaseMutaction];
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum DatabaseMutactionResultType {
    Create,
    Update,
    Upsert,
    Delete,
    Many,
    Unit,
}

#[derive(Debug, Clone)]
pub enum DatabaseMutaction {
    TopLevel(TopLevelDatabaseMutaction),
    Nested(NestedDatabaseMutaction),
}

impl DatabaseMutaction {
    pub fn typ(&self) -> DatabaseMutactionResultType {
        match self {
            DatabaseMutaction::TopLevel(tl) => match tl {
                TopLevelDatabaseMutaction::CreateNode(_) => DatabaseMutactionResultType::Create,
                TopLevelDatabaseMutaction::UpdateNode(_) => DatabaseMutactionResultType::Update,
                TopLevelDatabaseMutaction::UpsertNode(_) => DatabaseMutactionResultType::Upsert,
                TopLevelDatabaseMutaction::DeleteNode(_) => DatabaseMutactionResultType::Delete,
                TopLevelDatabaseMutaction::UpdateNodes(_) => DatabaseMutactionResultType::Many,
                TopLevelDatabaseMutaction::DeleteNodes(_) => DatabaseMutactionResultType::Many,
                TopLevelDatabaseMutaction::ResetData(_) => DatabaseMutactionResultType::Unit,
            },
            DatabaseMutaction::Nested(tl) => match tl {
                NestedDatabaseMutaction::CreateNode(_) => DatabaseMutactionResultType::Create,
            },
        }
    }
}

#[derive(Debug, Clone)]
pub enum TopLevelDatabaseMutaction {
    CreateNode(CreateNode),
    UpdateNode(UpdateNode),
    UpsertNode(TopLevelUpsertNode),
    DeleteNode(TopLevelDeleteNode),
    UpdateNodes(TopLevelUpdateNodes),
    DeleteNodes(TopLevelDeleteNodes),
    ResetData(ResetData),
}

#[derive(Debug, Clone)]
pub enum NestedDatabaseMutaction {
    CreateNode(NestedCreateNode),
}

#[derive(Debug, Default, Clone)]
pub struct NestedMutactions {
    pub creates: Vec<NestedCreateNode>,
    /*
    pub updates: Vec<NestedUpdateNode>,
    pub upserts: Vec<NestedUpsertNode>,
    pub deletes: Vec<NestedDeleteNode>,
    pub connects: Vec<NestedConnect>,
    pub sets: Vec<NestedSet>,
    pub disconnects: Vec<NestedDisconnect>,
    pub update_manys: Vec<NestedUpdateNodes>,
    pub delete_manys: Vec<NestedDeleteNodes>,
    */
}

// UPDATE

#[derive(Debug, Clone)]
pub struct TopLevelUpdateNodes {
    pub model: ModelRef,
    pub filter: Filter,
    pub non_list_args: PrismaArgs,
    pub list_args: Vec<(String, PrismaListValue)>,
}

// UPSERT

#[derive(Debug, Clone)]
pub struct TopLevelUpsertNode {
    pub where_: NodeSelector,
    pub create: CreateNode,
    pub update: UpdateNode,
}

// #[derive(Debug, Default, Clone, PartialEq)]
// pub struct NestedUpsertNode {
//     pub project: ProjectRef,
//     pub where_: NodeSelector,
//     pub create: NestedCreateNode,
//     pub update: NestedUpdateNode,

//     pub relation_field: Arc<RelationField>,
// }

// DELETE

#[derive(Debug, Clone)]
pub struct TopLevelDeleteNode {
    pub where_: NodeSelector,
}

/*
#[derive(Debug, Default, Clone, PartialEq)]
pub struct NestedDeleteNode {
    pub project: ProjectRef,
    pub where_: NodeSelector,

    pub relation_field: Arc<RelationField>,
}
*/

#[derive(Debug, Clone)]
pub struct TopLevelDeleteNodes {
    pub model: ModelRef,
    pub filter: Filter,
}

// CONNECT

// #[derive(Debug, Default, Clone, PartialEq)]
// pub struct NestedConnect {
//     pub project: ProjectRef,
//     pub where_: NodeSelector,

//     pub relation_field: Arc<RelationField>,
//     pub top_is_create: bool,
// }

// SET

// #[derive(Debug, Default, Clone, PartialEq)]
// pub struct NestedSet {
//     pub project: ProjectRef,
//     pub wheres: Vec<NodeSelector>,

//     pub relation_field: Arc<RelationField>,
// }

// DISCONNECT

// #[derive(Debug, Default, Clone, PartialEq)]
// pub struct NestedDisconnect {
//     pub project: ProjectRef,
//     pub where_: Option<NodeSelector>,

//     pub relation_field: Arc<RelationField>,
// }

#[derive(Debug, Clone)]
pub struct ResetData {
    pub project: ProjectRef,
}
