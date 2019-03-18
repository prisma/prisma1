use crate::query_ast;
use connector::{DataResolver, QueryArguments};
use prisma_common::PrismaResult;
use prisma_models::{GraphqlId, ManyNodes, SingleNode};
use query_ast::*;
use std::boxed::Box;
use std::sync::Arc;

pub struct QueryExecutor {
    pub data_resolver: Box<dyn DataResolver + Send + Sync + 'static>,
}

impl QueryExecutor {
    // WIP
    pub fn execute(&self, queries: &[PrismaQuery]) -> PrismaResult<Vec<PrismaQueryResult>> {
        self.execute_internal(queries, vec![])
    }

    fn execute_internal(
        &self,
        queries: &[PrismaQuery],
        parent_ids: Vec<GraphqlId>,
    ) -> PrismaResult<Vec<PrismaQueryResult>> {
        let mut results = vec![];
        for query in queries {
            match query {
                PrismaQuery::RecordQuery(query) => {
                    let result = self
                        .data_resolver
                        .get_node_by_where(&query.selector, &query.selected_fields)?;

                    match result {
                        Some(ref node) => {
                            let model = Arc::clone(&query.selector.field.model());
                            let ids = vec![node.get_id_value(model).clone()];

                            let nested_results = self.execute_internal(&query.nested, ids)?;

                            let result = SinglePrismaQueryResult {
                                name: query.name.clone(),
                                result: result,
                                nested: nested_results,
                            };
                            results.push(PrismaQueryResult::Single(result));
                        }
                        None => (),
                    }
                }
                PrismaQuery::RelatedRecordQuery(query) => {
                    let result = self.data_resolver.get_related_nodes(
                        Arc::clone(&query.parent_field),
                        &parent_ids,
                        QueryArguments::empty(),
                        &query.selected_fields,
                    )?;
                    let result = SinglePrismaQueryResult {
                        name: query.name.clone(),
                        result: result.into_single_node(),
                        nested: vec![],
                    };
                    results.push(PrismaQueryResult::Single(result));
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
    pub name: String,
    pub result: Option<SingleNode>,
    pub nested: Vec<PrismaQueryResult>,
}

#[derive(Debug)]
pub struct MultiPrismaQueryResult {
    pub query: MultiRecordQuery,
    pub result: ManyNodes,
}
