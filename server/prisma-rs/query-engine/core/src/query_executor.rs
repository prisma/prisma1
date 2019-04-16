#![allow(warnings)]

use crate::{query_ast, CoreResult};
use connector::{ConnectorResult, DataResolver, ScalarListValues};
use prisma_models::{GraphqlId, ManyNodes, ScalarField, SelectedFields, SingleNode};
use query_ast::*;
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

    /// Scalar list field names mapped to their results
    pub list_results: Vec<(String, Vec<ScalarListValues>)>,

    /// Used for filtering implicit fields in result node
    selected_fields: SelectedFields,
}

#[derive(Debug)]
pub struct MultiPrismaQueryResult {
    pub name: String,
    pub result: ManyNodes,
    pub nested: Vec<PrismaQueryResult>,

    /// Scalar list field names mapped to their results
    pub list_results: Vec<(String, Vec<ScalarListValues>)>,

    /// Used for filtering implicit fields in result nodes
    selected_fields: SelectedFields,
}

impl PrismaQueryResult {
    /// Filters implicitly selected fields from the result set.
    pub fn filter(self) -> Self {
        match self {
            PrismaQueryResult::Single(s) => PrismaQueryResult::Single(s.filter()),
            PrismaQueryResult::Multi(m) => PrismaQueryResult::Multi(m.filter()),
        }
    }
}

// Q: Best pattern here? Mix of in place mutation and recreating result
impl SinglePrismaQueryResult {
    /// Filters implicitly selected fields in-place in the result node and field names.
    /// Traverses nested result tree.
    pub fn filter(self) -> Self {
        let implicit_fields = self.selected_fields.get_implicit_fields();

        let result = self.result.map(|mut r| {
            let positions: Vec<usize> = implicit_fields
                .into_iter()
                .filter_map(|implicit| r.field_names.iter().position(|name| &implicit.field.name == name))
                .collect();

            positions.into_iter().for_each(|p| {
                r.field_names.remove(p);
                r.node.values.remove(p);
            });

            r
        });

        let nested = self.nested.into_iter().map(|nested| nested.filter()).collect();

        Self { result, nested, ..self }
    }
}

impl MultiPrismaQueryResult {
    /// Filters implicitly selected fields in-place in the result nodes and field names.
    /// Traverses nested result tree.
    pub fn filter(mut self) -> Self {
        let implicit_fields = self.selected_fields.get_implicit_fields();
        let positions: Vec<usize> = implicit_fields
            .into_iter()
            .filter_map(|implicit| {
                self.result
                    .field_names
                    .iter()
                    .position(|name| &implicit.field.name == name)
            })
            .collect();

        positions.iter().for_each(|p| {
            self.result.field_names.remove(p.clone());
        });

        // Remove values on found positions from all nodes.
        let nodes = self
            .result
            .nodes
            .into_iter()
            .map(|mut node| {
                positions.iter().for_each(|p| {
                    node.values.remove(p.clone());
                });
                node
            })
            .collect();

        let result = ManyNodes { nodes, ..self.result };
        let nested = self.nested.into_iter().map(|nested| nested.filter()).collect();

        Self { result, nested, ..self }
    }
}

pub struct QueryExecutor {
    pub data_resolver: Arc<DataResolver + Send + Sync + 'static>,
}

impl QueryExecutor {
    // WIP
    pub fn execute(&self, queries: &[PrismaQuery]) -> CoreResult<Vec<PrismaQueryResult>> {
        self.execute_internal(queries, vec![])
    }

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
                        .get_node_by_where(&query.selector, &selected_fields)?;

                    match result {
                        Some(ref node) => {
                            let model = Arc::clone(&query.selector.field.model());
                            let ids = vec![node.get_id_value(model)?.clone()];
                            let list_fields = selected_fields.scalar_lists();
                            let list_results = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
                            let nested = self.execute_internal(&query.nested, ids)?;
                            let result = SinglePrismaQueryResult {
                                name: query.name.clone(),
                                result,
                                nested,
                                selected_fields,
                                list_results,
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

                    let ids = result.get_id_values(Arc::clone(&query.model))?;
                    let list_fields = selected_fields.scalar_lists();
                    let list_results = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;

                    // FIXME: Rewrite to not panic and also in a more functional way!
                    let nested = result.nodes.iter().fold(vec![], |mut vec, _| {
                        vec.append(&mut self.execute_internal(&query.nested, ids.clone()).unwrap());
                        vec
                    });

                    results.push(PrismaQueryResult::Multi(MultiPrismaQueryResult {
                        name: query.name.clone(),
                        result,
                        nested,
                        selected_fields,
                        list_results,
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
                        let list_fields = selected_fields.scalar_lists();
                        let list_results = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
                        let nested = self.execute_internal(&query.nested, ids)?;
                        let result = SinglePrismaQueryResult {
                            name: query.name.clone(),
                            result: Some(node),
                            nested,
                            selected_fields,
                            list_results,
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
                    let list_fields = selected_fields.scalar_lists();
                    let list_results = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
                    let nested = result.nodes.iter().fold(vec![], |mut vec, _| {
                        vec.append(&mut self.execute_internal(&query.nested, ids.clone()).unwrap());
                        vec
                    });

                    results.push(PrismaQueryResult::Multi(MultiPrismaQueryResult {
                        name: query.name.clone(),
                        result,
                        nested,
                        selected_fields,
                        list_results,
                    }));
                }
            }
        }

        Ok(results)
    }

    fn resolve_scalar_list_fields(
        &self,
        node_ids: Vec<GraphqlId>,
        list_fields: Vec<Arc<ScalarField>>,
    ) -> ConnectorResult<Vec<(String, Vec<ScalarListValues>)>> {
        if !list_fields.is_empty() {
            list_fields
                .into_iter()
                .map(|list_field| {
                    let name = list_field.name.clone();
                    self.data_resolver
                        .get_scalar_list_values_by_node_ids(list_field, node_ids.clone())
                        .map(|r| (name, r))
                })
                .collect::<ConnectorResult<Vec<(String, Vec<_>)>>>()
        } else {
            Ok(vec![])
        }
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
