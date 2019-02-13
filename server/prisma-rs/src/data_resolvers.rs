mod sqlite;

pub use sqlite::Sqlite;
pub type PrismaDataResolver = Box<dyn DataResolver + Send + Sync + 'static>;

use crate::{models::prelude::*, protobuf::prelude::*, PrismaResult};

use sql::prelude::*;
use std::collections::BTreeSet;

pub struct SelectQuery {
    pub project: Project,
    pub model_name: String,
    pub selected_fields: BTreeSet<String>,
    pub conditions: ConditionTree,
    pub order_by: Option<u32>, // TODO: add a proper order by structure
    pub skip: Option<u32>,
    pub after: Option<GraphqlId>,
    pub first: Option<u32>,
}

pub trait IntoSelectQuery {
    fn into_select_query(self) -> PrismaResult<SelectQuery>;
}

pub trait DataResolver {
    fn select_nodes(&self, query: SelectQuery) -> PrismaResult<(Vec<Node>, Vec<String>)>;
}
