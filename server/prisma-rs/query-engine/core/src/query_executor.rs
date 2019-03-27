use crate::{query_ast, CoreResult};
use connector::DataResolver;
use prisma_models::{GraphqlId, ManyNodes, SingleNode};
use query_ast::*;
use std::boxed::Box;
use std::sync::Arc;

pub struct QueryExecutor {
    pub data_resolver: Box<dyn DataResolver + Send + Sync + 'static>,
}

impl QueryExecutor {
    // WIP
    pub fn execute(&self, queries: &[PrismaQuery]) -> CoreResult<Vec<PrismaQueryResult>> {
        self.execute_internal(queries, vec![])
    }

    #[allow(unused_variables)]
    fn execute_internal(
        &self,
        queries: &[PrismaQuery],
        parent_ids: Vec<GraphqlId>,
    ) -> CoreResult<Vec<PrismaQueryResult>> {
        let mut results = vec![];
        for query in queries {
            match query {
                PrismaQuery::RecordQuery(query) => {
                    let result = self
                        .data_resolver
                        .get_node_by_where(query.selector.clone(), &query.selected_fields)?;

                    match result {
                        Some(ref node) => {
                            let model = Arc::clone(&query.selector.field.model());
                            let ids = vec![node.get_id_value(model)?.clone()];

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
                PrismaQuery::MultiRecordQuery(query) => {
                    let result = self.data_resolver.get_nodes(
                        Arc::clone(&query.model),
                        query.args.clone(),
                        query.selected_fields.clone(),
                    )?;

                    // FIXME: Rewrite to not panic and also in a more functional way!
                    let ids = result.get_id_values(Arc::clone(&query.model))?;
                    let nested = result.nodes.iter().fold(vec![], |mut vec, node| {
                        vec.append(&mut self.execute_internal(&query.nested, ids.clone()).unwrap());
                        vec
                    });

                    results.push(PrismaQueryResult::Multi(MultiPrismaQueryResult {
                        name: query.name.clone(),
                        result,
                        nested,
                    }));
                }
                PrismaQuery::RelatedRecordQuery(query) => {
                    let result = self.data_resolver.get_related_nodes(
                        Arc::clone(&query.parent_field),
                        &parent_ids,
                        query.args.clone(),
                        &query.selected_fields,
                    )?;

                    // FIXME: Required fields need to return Errors, non-required can be ignored!
                    if let Some(node) = result.into_single_node() {
                        let ids = vec![node.get_id_value(query.parent_field.related_model())?.clone()];
                        let nested_results = self.execute_internal(&query.nested, ids)?;
                        let result = SinglePrismaQueryResult {
                            name: query.name.clone(),
                            result: Some(node),
                            nested: nested_results,
                        };
                        results.push(PrismaQueryResult::Single(result));
                    }
                }
                PrismaQuery::MultiRelatedRecordQuery(query) => {
                    let result = self.data_resolver.get_related_nodes(
                        Arc::clone(&query.parent_field),
                        &parent_ids,
                        query.args.clone(),
                        &query.selected_fields,
                    )?;

                    // FIXME: Rewrite to not panic and also in a more functional way!
                    let ids = result.get_id_values(Arc::clone(&query.parent_field.related_model()))?;
                    let nested = result.nodes.iter().fold(vec![], |mut vec, node| {
                        vec.append(&mut self.execute_internal(&query.nested, ids.clone()).unwrap());
                        vec
                    });

                    results.push(PrismaQueryResult::Multi(MultiPrismaQueryResult {
                        name: query.name.clone(),
                        result,
                        nested,
                    }));
                }
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
    pub name: String,
    pub result: ManyNodes,
    pub nested: Vec<PrismaQueryResult>,
}
