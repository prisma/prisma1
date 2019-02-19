mod sqlite;

pub use sqlite::Sqlite;
pub type PrismaDataResolver = Box<dyn DataResolver + Send + Sync + 'static>;

use crate::{models::prelude::*, protobuf::prelude::*, PrismaResult};

use sql::prelude::*;
use std::collections::BTreeSet;

#[derive(Debug)]
pub struct SelectQuery {
    pub project: ProjectRef,
    pub model: ModelRef,
    pub selected_fields: BTreeSet<String>,
    pub conditions: ConditionTree,
    pub order_by: Option<usize>,
    pub skip: Option<usize>,
    pub first: Option<usize>,
    pub last: Option<usize>,
}

pub trait IntoSelectQuery {
    fn into_select_query(self) -> PrismaResult<SelectQuery>;
}

pub trait DataResolver {
    fn select_nodes(&self, query: SelectQuery) -> PrismaResult<(Vec<Node>, Vec<String>)>;
}
