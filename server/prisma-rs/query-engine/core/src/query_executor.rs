use crate::query_ast;
use connector::DataResolver;
use prisma_common::PrismaResult;
use prisma_models::{ManyNodes, SingleNode};
use query_ast::*;
use std::boxed::Box;
use std::sync::Arc;

pub struct QueryExecutor {
    pub data_resolver: Box<dyn DataResolver + Send + Sync + 'static>,
}

impl QueryExecutor {
    // WIP
    pub fn execute(&self, queries: Vec<PrismaQuery>) -> PrismaResult<Vec<PrismaQueryResult>> {
        let mut results = vec![];
        for query in queries {
            match query {
                PrismaQuery::RecordQuery(query) => {
                    let result = self
                        .data_resolver
                        .get_node_by_where(&query.selector, &query.selected_fields)?;

                    results.push(PrismaQueryResult::Single(SinglePrismaQueryResult { query, result }));
                }
                _ => unimplemented!(),
            }
        }

        Ok(results)
    }
}

#[derive(Debug)]
pub enum PrismaQueryResult {
    Single(SinglePrismaQueryResult),
    Multi(MultiPrismaQueryResult),
}

#[derive(Debug)]
pub struct SinglePrismaQueryResult {
    pub query: RecordQuery,
    pub result: Option<SingleNode>,
}

#[derive(Debug)]
pub struct MultiPrismaQueryResult {
    pub query: MultiRecordQuery,
    pub result: ManyNodes,
}
