use crate::CoreResult;
use connector::{self, query_ast::*, result_ast::*, ManagedDatabaseReader, ScalarListValues};
use prisma_models::{GraphqlId, ScalarField, SelectedFields};
use std::sync::Arc;

pub struct ReadQueryExecutor {
    pub data_resolver: Arc<dyn ManagedDatabaseReader + Send + Sync + 'static>,
}

impl ReadQueryExecutor {
    pub fn execute(&self, query: ReadQuery, parent_ids: &[GraphqlId]) -> CoreResult<ReadQueryResult> {
        match query {
            ReadQuery::RecordQuery(q) => self.read_one(q),
            ReadQuery::ManyRecordsQuery(q) => self.read_many(q),
            ReadQuery::RelatedRecordsQuery(q) => self.read_related(q, parent_ids),
        }
    }

    /// Queries a single record.
    pub fn read_one(&self, query: RecordQuery) -> CoreResult<ReadQueryResult> {
        let selected_fields = Self::inject_required_fields(query.selected_fields.clone());
        let scalars = self
            .data_resolver
            .get_single_record(query.record_finder.as_ref().unwrap(), &selected_fields)?;

        let model = Arc::clone(&query.record_finder.unwrap().field.model());
        let id_field = model.fields().id().name.clone();

        match scalars {
            Some(record) => {
                let ids = vec![record.collect_id(&id_field)?];
                let list_fields = selected_fields.scalar_lists();
                let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
                let nested: Vec<ReadQueryResult> = query
                    .nested
                    .into_iter()
                    .map(|q| self.execute(q, &ids))
                    .collect::<CoreResult<Vec<ReadQueryResult>>>()?;

                Ok(ReadQueryResult {
                    name: query.name,
                    alias: query.alias,
                    fields: query.selection_order,
                    scalars: record.into(),
                    nested,
                    lists,
                    id_field,
                    ..Default::default()
                })
            }
            None => Ok(ReadQueryResult {
                name: query.name,
                alias: query.alias,
                fields: query.selection_order,
                id_field,
                ..Default::default()
            }),
        }
    }

    /// Queries a set of records.
    pub fn read_many(&self, query: ManyRecordsQuery) -> CoreResult<ReadQueryResult> {
        let selected_fields = Self::inject_required_fields(query.selected_fields.clone());
        let scalars =
            self.data_resolver
                .get_many_records(Arc::clone(&query.model), query.args.clone(), &selected_fields)?;

        let model = Arc::clone(&query.model);
        let id_field = model.fields().id().name.clone();
        let ids = scalars.collect_ids(&id_field)?;
        let list_fields = selected_fields.scalar_lists();
        let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
        let nested: Vec<ReadQueryResult> = query
            .nested
            .into_iter()
            .map(|q| self.execute(q, &ids))
            .collect::<CoreResult<Vec<ReadQueryResult>>>()?;

        Ok(ReadQueryResult {
            name: query.name,
            alias: query.alias,
            fields: query.selection_order,
            query_arguments: query.args,
            scalars,
            nested,
            lists,
            id_field,
        })
    }

    /// Queries related records for a set of parent IDs.
    pub fn read_related(&self, query: RelatedRecordsQuery, parent_ids: &[GraphqlId]) -> CoreResult<ReadQueryResult> {
        let selected_fields = Self::inject_required_fields(query.selected_fields.clone());
        let scalars = self.data_resolver.get_related_records(
            Arc::clone(&query.parent_field),
            parent_ids,
            query.args.clone(),
            &selected_fields,
        )?;

        let model = Arc::clone(&query.parent_field.related_model());
        let id_field = model.fields().id().name.clone();
        let ids = scalars.collect_ids(&id_field)?;
        let list_fields = selected_fields.scalar_lists();
        let lists = self.resolve_scalar_list_fields(ids.clone(), list_fields)?;
        let nested: Vec<ReadQueryResult> = query
            .nested
            .into_iter()
            .map(|q| self.execute(q, &ids))
            .collect::<CoreResult<Vec<ReadQueryResult>>>()?;

        Ok(ReadQueryResult {
            name: query.name,
            alias: query.alias,
            fields: query.selection_order,
            query_arguments: query.args,
            scalars,
            nested,
            lists,
            id_field,
        })
    }

    /// Resolves scalar lists for a list field for a set of parent IDs.
    fn resolve_scalar_list_fields(
        &self,
        record_ids: Vec<GraphqlId>,
        list_fields: Vec<Arc<ScalarField>>,
    ) -> connector::Result<Vec<(String, Vec<ScalarListValues>)>> {
        if !list_fields.is_empty() {
            list_fields
                .into_iter()
                .map(|list_field| {
                    let name = list_field.name.clone();
                    self.data_resolver
                        .get_scalar_list_values_by_record_ids(list_field, record_ids.clone())
                        .map(|r| (name, r))
                })
                .collect::<connector::Result<Vec<(String, Vec<_>)>>>()
        } else {
            Ok(vec![])
        }
    }

    /// Injects fields required for querying, if they're not already in the selection set.
    /// Currently, required fields for every query are:
    /// - ID field
    fn inject_required_fields(mut selected_fields: SelectedFields) -> SelectedFields {
        let id_field = selected_fields.model().fields().id();

        if selected_fields.scalar.iter().find(|f| f.field.name == id_field.name).is_none() {
            selected_fields.add_scalar(id_field);
        }

        selected_fields
    }
}
