use crate::query_ast;
use connector::DataResolver;
use prisma_common::PrismaResult;
use prisma_models::SingleNode;
use query_ast::*;

pub struct QueryExecutor {
    data_resolver: DataResolver,
}

impl QueryExecutor {
    // WIP
    pub fn execute(&self, queries: &mut Vec<PrismaQuery>) -> PrismaResult<Option<SingleNode>> {
        queries.reverse();
        let query = queries.pop().unwrap();
        match query {
            PrismaQuery::RecordQuery(query) => self
                .data_resolver
                .get_node_by_where(query.selector, query.selected_fields),
            _ => unimplemented!(),
        }
    }
}
