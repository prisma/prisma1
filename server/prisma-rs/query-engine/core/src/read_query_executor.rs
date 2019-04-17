use crate::{query_ast, results::*, CoreResult};
use connector::{ConnectorResult, DataResolver, ScalarListValues};
use prisma_models::{GraphqlId, ScalarField, SelectedFields};
use query_ast::*;
use std::sync::Arc;

pub struct ReadQueryExecutor {
    pub data_resolver: Arc<DataResolver + Send + Sync + 'static>,
}

impl ReadQueryExecutor {
    pub fn execute(&self, queries: &[ReadQuery]) -> CoreResult<Vec<ReadQueryResult>> {
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

                    dbg!(&scalars);

                    let ids = scalars.get_id_values(Arc::clone(&query.model))?;
                    let list_fields = selected_fields.scalar_lists();
                    let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;

                    // FIXME: Rewrite to not panic and also in a more functional way!
                    let nested = scalars.nodes.iter().fold(vec![], |mut vec, _| {
                        vec.append(&mut self.execute_internal(&query.nested, ids.clone()).unwrap());
                        vec
                    });

                    results.push(ReadQueryResult::Many(ManyReadQueryResults {
                        name: query.name.clone(),
                        fields: query.fields.clone(),
                        scalars,
                        nested,
                        selected_fields,
                        lists,
                        query_arguments: query.args.clone(),
                    }));
                }
                ReadQuery::RelatedRecordQuery(query) => {
                    let selected_fields = Self::inject_required_fields(query.selected_fields.clone());

                    let result = self.data_resolver.get_related_nodes(
                        Arc::clone(&query.parent_field),
                        &parent_ids,
                        query.args.clone(),
                        &selected_fields,
                    )?;

                    // FIXME: Required fields need to return Errors, non-required can be ignored!
                    if let Some(record) = result.into_single_node() {
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
                    let nested = scalars.nodes.iter().fold(vec![], |mut vec, _| {
                        vec.append(&mut self.execute_internal(&query.nested, ids.clone()).unwrap());
                        vec
                    });

                    results.push(ReadQueryResult::Many(ManyReadQueryResults {
                        name: query.name.clone(),
                        fields: query.fields.clone(),
                        scalars,
                        nested,
                        selected_fields,
                        lists,
                        query_arguments: query.args.clone(),
                    }));
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
