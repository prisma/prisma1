use crate::query_ast;
use connector::DataResolver;
use prisma_common::PrismaResult;
use prisma_models::SingleNode;
use query_ast::*;
use std::boxed::Box;
use std::sync::Arc;

pub struct QueryExecutor {
    pub data_resolver: Box<dyn DataResolver + Send + Sync + 'static>,
}

impl QueryExecutor {
    // WIP
    pub fn execute(&self, queries: Vec<PrismaQuery>) -> PrismaResult<Option<SingleNode>> {
        let query = queries.into_iter().next().unwrap();
        match query {
            PrismaQuery::RecordQuery(query) => self
                .data_resolver
                .get_node_by_where(query.selector, query.selected_fields),
            _ => unimplemented!(),
        }
    }
}
