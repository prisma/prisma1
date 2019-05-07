use crate::{query_ast, query_results::*, CoreResult};
use connector::{ConnectorResult, DataResolver, ScalarListValues};
use prisma_models::{GraphqlId, ScalarField, SelectedFields, SingleNode};
use query_ast::*;
use std::{convert::TryFrom, sync::Arc};

pub struct ReadQueryExecutor {
    pub data_resolver: Arc<DataResolver + Send + Sync + 'static>,
}

impl ReadQueryExecutor {
    pub fn execute(&self, queries: &[ReadQuery]) -> CoreResult<Vec<ReadQueryResult>> {
        dbg!(queries);
        self.execute_internal(queries, vec![])
    }

    fn execute_internal(&self, queries: &[ReadQuery], parent_ids: Vec<GraphqlId>) -> CoreResult<Vec<ReadQueryResult>> {
        let mut results = vec![];

        for query in queries {
            match query {
                ReadQuery::RecordQuery(query) => {
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());

                    let scalars = self
                        .data_resolver
                        .get_node_by_where(&query.selector, &selected_fields)?;

                    match scalars {
                        Some(ref record) => {
                            let model = Arc::clone(&query.selector.field.model());
                            let ids = vec![record.get_id_value(model)?.clone()];
                            let list_fields = selected_fields.scalar_lists();
                            let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
                            let nested = self.execute_internal(&query.nested, ids)?;

                            let result = SingleReadQueryResult {
                                name: query.name.clone(),
                                fields: query.fields.clone(),
                                scalars,
                                nested,
                                selected_fields,
                                lists,
                            };
                            results.push(ReadQueryResult::Single(result));
                        }
                        None => (),
                    }
                }
                ReadQuery::ManyRecordsQuery(query) => {
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());
                    let scalars =
                        self.data_resolver
                            .get_nodes(Arc::clone(&query.model), query.args.clone(), &selected_fields)?;

                    let ids = scalars.get_id_values(Arc::clone(&query.model))?;
                    let list_fields = selected_fields.scalar_lists();
                    let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
                    let nested = self.execute_internal(&query.nested, ids.clone())?;

                    results.push(ReadQueryResult::Many(ManyReadQueryResults::new(
                        query.name.clone(),
                        query.fields.clone(),
                        scalars,
                        nested,
                        lists,
                        query.args.clone(),
                        selected_fields,
                    )));
                }
                ReadQuery::RelatedRecordQuery(query) => {
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());

                    let result = self.data_resolver.get_related_nodes(
                        Arc::clone(&query.parent_field),
                        &parent_ids,
                        query.args.clone(),
                        &selected_fields,
                    )?;

                    // If our result set contains more than one entry
                    // we need to handle all of them!
                    if result.nodes.len() > 1 {
                        for node in result.nodes.into_iter() {
                            let single = SingleNode {
                                node,
                                field_names: result.field_names.clone(),
                            };

                            let ids = vec![single.get_id_value(query.parent_field.related_model())?.clone()];
                            let nested = self.execute_internal(&query.nested, ids.clone())?;
                            let list_fields = selected_fields.scalar_lists();
                            let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;

                            let result = SingleReadQueryResult {
                                name: query.name.clone(),
                                fields: query.fields.clone(),
                                scalars: Some(single),
                                nested,
                                selected_fields: selected_fields.clone(),
                                lists,
                            };

                            results.push(ReadQueryResult::Single(result));
                        }
                    }
                    // FIXME: Required fields need to return Errors, non-required can be ignored!
                    else if let Ok(record) = SingleNode::try_from(result) {
                        let ids = vec![record.get_id_value(query.parent_field.related_model())?.clone()];
                        let list_fields = selected_fields.scalar_lists();
                        let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
                        let nested = self.execute_internal(&query.nested, ids)?;
                        let result = SingleReadQueryResult {
                            name: query.name.clone(),
                            fields: query.fields.clone(),
                            scalars: Some(record),
                            nested,
                            selected_fields,
                            lists,
                        };
                        results.push(ReadQueryResult::Single(result));
                    } else {
                        results.push(ReadQueryResult::Single(SingleReadQueryResult {
                            name: query.name.clone(),
                            fields: query.fields.clone(),
                            scalars: None,
                            nested: vec![],
                            selected_fields,
                            lists: vec![],
                        }));
                    }
                }
                ReadQuery::ManyRelatedRecordsQuery(query) => {
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());

                    let scalars = self.data_resolver.get_related_nodes(
                        Arc::clone(&query.parent_field),
                        &parent_ids,
                        query.args.clone(),
                        &selected_fields,
                    )?;

                    // FIXME: Rewrite to not panic and also in a more functional way!
                    let ids = scalars.get_id_values(Arc::clone(&query.parent_field.related_model()))?;
                    let list_fields = selected_fields.scalar_lists();
                    let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
                    let nested = self.execute_internal(&query.nested, ids.clone())?;

                    results.push(ReadQueryResult::Many(ManyReadQueryResults::new(
                        query.name.clone(),
                        query.fields.clone(),
                        scalars,
                        nested,
                        lists,
                        query.args.clone(),
                        selected_fields,
                    )));
                }
            }
        }

        Ok(results)
    }

    fn resolve_scalar_list_fields(
        &self,
        record_ids: Vec<GraphqlId>,
        list_fields: Vec<Arc<ScalarField>>,
    ) -> ConnectorResult<Vec<(String, Vec<ScalarListValues>)>> {
        if !list_fields.is_empty() {
            list_fields
                .into_iter()
                .map(|list_field| {
                    let name = list_field.name.clone();
                    self.data_resolver
                        .get_scalar_list_values_by_node_ids(list_field, record_ids.clone())
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
