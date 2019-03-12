mod sqlite;

use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use prisma_query::ast::Select;
use rusqlite::Row;

pub use sqlite::Sqlite;

#[derive(Debug)]
pub struct SelectQuery {
    pub db_name: String,
    pub query_ast: Select,
    pub selected_fields: SelectedFields,
}

pub trait IntoSelectQuery {
    fn into_select_query(self) -> PrismaResult<SelectQuery>;
}

pub trait DatabaseExecutor {
    fn with_rows<F>(&self, query: Select, db_name: String, f: F) -> PrismaResult<Vec<Node>>
    where
        F: FnMut(&Row) -> Node;
}
