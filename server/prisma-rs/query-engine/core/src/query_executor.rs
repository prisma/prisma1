use crate::{query_ast, CoreResult};
use connector::DataResolver;
use prisma_models::{GraphqlId, ManyNodes, SelectedFields, SingleNode};
use query_ast::*;
use std::boxed::Box;
use std::sync::Arc;

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

    /// Used for filtering implicit fields in result node
    selected_fields: SelectedFields,
}

#[derive(Debug)]
pub struct MultiPrismaQueryResult {
    pub name: String,
    pub result: ManyNodes,
    pub nested: Vec<PrismaQueryResult>,

    /// Used for filtering implicit fields in result nodes
    selected_fields: SelectedFields,
}

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
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());

                    let result = self
                        .data_resolver
                        .get_node_by_where(query.selector.clone(), &selected_fields)?;

                    match result {
                        Some(ref node) => {
                            let model = Arc::clone(&query.selector.field.model());
                            let ids = vec![node.get_id_value(model)?.clone()];

                            let nested = self.execute_internal(&query.nested, ids)?;
                            let result = SinglePrismaQueryResult {
                                name: query.name.clone(),
                                result,
                                nested,
                                selected_fields,
                            };
                            results.push(PrismaQueryResult::Single(result));
                        }
                        None => (),
                    }
                }
                PrismaQuery::MultiRecordQuery(query) => {
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());

                    let result =
                        self.data_resolver
                            .get_nodes(Arc::clone(&query.model), query.args.clone(), &selected_fields)?;

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
                        selected_fields,
                    }));
                }
                PrismaQuery::RelatedRecordQuery(query) => {
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());

                    let result = self.data_resolver.get_related_nodes(
                        Arc::clone(&query.parent_field),
                        &parent_ids,
                        query.args.clone(),
                        &selected_fields,
                    )?;

                    // FIXME: Required fields need to return Errors, non-required can be ignored!
                    if let Some(node) = result.into_single_node() {
                        let ids = vec![node.get_id_value(query.parent_field.related_model())?.clone()];
                        let nested = self.execute_internal(&query.nested, ids)?;
                        let result = SinglePrismaQueryResult {
                            name: query.name.clone(),
                            result: Some(node),
                            nested,
                            selected_fields,
                        };
                        results.push(PrismaQueryResult::Single(result));
                    }
                }
                PrismaQuery::MultiRelatedRecordQuery(query) => {
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());

                    let result = self.data_resolver.get_related_nodes(
                        Arc::clone(&query.parent_field),
                        &parent_ids,
                        query.args.clone(),
                        &selected_fields,
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
                        selected_fields,
                    }));
                }
            }
        }

        Ok(results)
    }

    /// Injects fields required for querying, if they're not already in the selection set.
    /// Currently, required fields for every query are:
    /// - ID field
    fn inject_required_fields(mut selected_fields: SelectedFields) -> SelectedFields {
        let id_field = selected_fields.model().fields().id();
        if let None = selected_fields.scalar.iter().find(|f| f.field.name == id_field.name) {
            selected_fields.add_scalar(id_field.into(), true);
        };

        selected_fields
    }
}
