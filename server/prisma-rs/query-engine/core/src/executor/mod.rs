//! A slightly more generic interface over executing read and write queries

#![allow(warnings)]

mod read;
mod write;
mod pipeline;

use self::pipeline::*;

pub use read::ReadQueryExecutor;
pub use write::WriteQueryExecutor;

use crate::{
    BuilderExt, CoreError, CoreResult, Query, ReadQuery, ReadQueryResult, RecordQuery, SingleBuilder, WriteQuery,
    WriteQueryResult,
};
use connector::{filter::NodeSelector, QueryArguments};
use connector::{
    mutaction::{DatabaseMutactionResult, TopLevelDatabaseMutaction},
    ConnectorResult,
};

use std::sync::Arc;

use graphql_parser::query::{Field, Selection, Value};
use prisma_models::{
    Field as ModelField, GraphqlId, ModelRef, OrderBy, PrismaValue, RelationFieldRef, InternalDataModelRef, SelectedField,
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
        let model = wq.model();

        let query: RecordQuery = SingleBuilder::new().setup(Arc::clone(&model), &wq.field).build()?;

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
}
