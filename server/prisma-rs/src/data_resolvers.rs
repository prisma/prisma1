mod sqlite;

pub use sqlite::Sqlite;
pub type PrismaDataResolver = Box<dyn DataResolver + Send + Sync + 'static>;

use crate::{models::prelude::*, ordering::OrderVec, protobuf::prelude::*, PrismaResult};

use prisma_query::ast::*;
use std::collections::BTreeSet;

#[derive(Debug)]
pub struct SelectQuery {
    pub project: ProjectRef,
    pub model: ModelRef,
    pub selected_fields: BTreeSet<String>,
    pub conditions: ConditionTree,
    pub ordering: Option<OrderVec>,
    pub skip: usize,
    pub limit: Option<usize>,
}

pub trait IntoSelectQuery {
    fn into_select_query(self) -> PrismaResult<SelectQuery>;
}

pub trait DataResolver {
    fn select_nodes(&self, query: SelectQuery) -> PrismaResult<(Vec<Node>, Vec<String>)>;
}
