mod sqlite;

use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use prisma_query::ast::*;
pub use sqlite::Sqlite;

pub type PrismaDataResolver = Box<dyn DataResolver + Send + Sync + 'static>;

#[derive(Debug)]
pub struct SelectResult {
    pub nodes: Vec<Node>,
    pub field_names: Vec<String>,
}

#[derive(Debug)]
pub struct SelectQuery {
    pub db_name: String,
    pub query_ast: Select,
    pub selected_fields: SelectedFields,
}

pub trait IntoSelectQuery {
    fn into_select_query(self) -> PrismaResult<SelectQuery>;
}

pub trait DataResolver {
    fn select_nodes(&self, query: SelectQuery) -> PrismaResult<SelectResult>;
}
