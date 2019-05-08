//! A slightly more generic interface over executing read and write queries

#![allow(warnings)]

mod read;
mod write;

pub use read::ReadQueryExecutor;
pub use write::WriteQueryExecutor;

use crate::{CoreError, CoreResult, Query, ReadQuery, ReadQueryResult, WriteQuery, WriteQueryResult, RecordQuery};
use connector::{mutaction::DatabaseMutactionResult, ConnectorResult};
use connector::{filter::NodeSelector, QueryArguments};

use std::sync::Arc;

use graphql_parser::query::{Field, Selection, Value};
use prisma_models::{
    Field as ModelField, GraphqlId, ModelRef, OrderBy, PrismaValue, RelationFieldRef, SchemaRef, SelectedField,
    SelectedFields, SelectedRelationField, SelectedScalarField, SortOrder,
};


/// A wrapper around QueryExecutor
pub struct Executor {
    pub read_exec: ReadQueryExecutor,
    pub write_exec: WriteQueryExecutor,
}

type FoldResult = ConnectorResult<Vec<DatabaseMutactionResult>>;

impl Executor {
    /// Can be given a list of both ReadQueries and WriteQueries
    ///
    /// Will execute WriteQueries first, then all ReadQueries, while preserving order.
    pub fn exec_all(&self, queries: Vec<Query>) -> CoreResult<Vec<ReadQueryResult>> {
        let (writes, mut reads) = Self::split_read_write(queries);

        // Every WriteQuery get's executed and then built into a ReadQuery
        let write_results = writes
            .into_iter()
            .map(|wq| self.exec_single_tree(wq))
            .collect::<CoreResult<Vec<WriteQueryResult>>>()?;

        // Re-insert writes into read-list
        Self::zip_read_query_lists(write_results, &mut reads);

        // The process all reads
        let reads: Vec<ReadQuery> = reads.into_iter().filter_map(|q| q).collect();
        self.read_exec.execute(reads.as_slice())
    }

    /// Executes a single WriteQuery
    fn exec_single_tree(&self, wq: WriteQuery) -> CoreResult<WriteQueryResult> {
        let result = self.write_exec.execute(wq.inner.clone())?;

        dbg!(&wq);
        dbg!(&result);

        use connector::mutaction::TopLevelDatabaseMutaction;
        use connector::mutaction::Identifier;
        use prisma_models::PrismaValue;

        let model = match wq.inner {
            TopLevelDatabaseMutaction::CreateNode(cn) => cn.model,
            _ => unimplemented!(),
        };

        let name = model.name.clone();
        let field = model.fields().find_from_scalar("id").unwrap();

        let graphqlid = match result.identifier {
            Identifier::Id(ref gqlid) => gqlid.clone(),
            _ => unimplemented!(),
        };

        let value = PrismaValue::GraphqlId(graphqlid);
        let selector = NodeSelector {
            field: Arc::clone(&field),
            value,
        };

        let selected_fields = Self::collect_selected_fields(Arc::clone(&model), &wq.field, None)?;
        let fields = Self::collect_selection_order(&wq.field);

        let query = RecordQuery {
            name,
            selector,
            selected_fields,
            nested: vec![],
            fields,
        };

        Ok(WriteQueryResult {
            inner: result,
            nested: vec![],
            query: ReadQuery::RecordQuery(query),
        })
    }

    fn split_read_write(queries: Vec<Query>) -> (Vec<WriteQuery>, Vec<Option<ReadQuery>>) {
        queries.into_iter().fold((vec![], vec![]), |(mut w, mut r), query| {
            match query {
                Query::Write(q) => {
                    w.push(q); // Push WriteQuery
                    r.push(None); // Push Read placeholder
                }
                Query::Read(q) => r.push(Some(q)),
            }

            (w, r)
        })
    }

    fn zip_read_query_lists(mut writes: Vec<WriteQueryResult>, reads: &mut Vec<Option<ReadQuery>>) {
        (0..reads.len()).for_each(|idx| {
            if reads.get(idx).unwrap().is_none() {
                reads.insert(idx, Some(writes.remove(0).query));
            }
        });
    }

    fn collect_selection_order(field: &Field) -> Vec<String> {
        field
            .selection_set
            .items
            .iter()
            .filter_map(|select| {
                if let Selection::Field(field) = select {
                    Some(field.alias.clone().unwrap_or_else(|| field.name.clone()))
                } else {
                    None
                }
            })
            .collect()
    }

    /// FIXME: Deduplicate code here
    fn collect_selected_fields<I: Into<Option<RelationFieldRef>>>(
        model: ModelRef,
        field: &Field,
        parent: I,
    ) -> CoreResult<SelectedFields> {
        field
            .selection_set
            .items
            .iter()
            .filter_map(|i| {
                if let Selection::Field(f) = i {
                    // We have to make sure the selected field exists in some form.
                    let field = model.fields().find_from_all(&f.name);
                    match field {
                        Ok(ModelField::Scalar(field)) => Some(Ok(SelectedField::Scalar(SelectedScalarField {
                            field: Arc::clone(&field),
                            implicit: false,
                        }))),
                        // Relation fields are not handled here, but in nested queries
                        Ok(ModelField::Relation(field)) => Some(Ok(SelectedField::Relation(SelectedRelationField {
                            field: Arc::clone(&field),
                            selected_fields: SelectedFields::new(vec![], None),
                        }))),
                        _ => Some(Err(CoreError::QueryValidationError(format!(
                            "Selected field {} not found on model {}",
                            f.name, model.name,
                        )))),
                    }
                } else {
                    Some(Err(CoreError::UnsupportedFeatureError(
                        "Fragments and inline fragment spreads.".into(),
                    )))
                }
            })
            .collect::<CoreResult<Vec<_>>>()
            .map(|sf| SelectedFields::new(sf, parent.into()))
    }
}

