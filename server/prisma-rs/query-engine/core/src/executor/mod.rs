mod pipeline;
mod read;
mod write;

pub use read::ReadQueryExecutor;
pub use write::WriteQueryExecutor;

use self::pipeline::*;
use crate::{query_ir::QueryDocument, CoreResult, QuerySchemaRef};
use connector::{Query, ReadQueryResult};

/// Central query executor and main entry point into the query core.
pub struct QueryExecutor {
    query_schema: QuerySchemaRef,
    read_executor: ReadQueryExecutor,
    write_executor: WriteQueryExecutor,
}

impl QueryExecutor {
    pub fn new(
        query_schema: QuerySchemaRef,
        read_executor: ReadQueryExecutor,
        write_executor: WriteQueryExecutor,
    ) -> Self {
        QueryExecutor {
            query_schema,
            read_executor,
            write_executor,
        }
    }

    pub fn execute(query_doc: QueryDocument) -> CoreResult<()> {
        // 1. Parse and validate query document (building)
        // 2. Build query plan
        // 3. Execute query plan

        unimplemented!()
    }

    /// Legacy path
    /// Can be given a list of both ReadQueries and WriteQueries
    ///
    /// Will execute WriteQueries first, then all ReadQueries, while preserving order.
    pub fn exec_all(&self, queries: Vec<Query>) -> CoreResult<Vec<ReadQueryResult>> {
        // Give all queries to the pipeline module
        let mut pipeline = QueryPipeline::from(queries);

        // Execute prefetch queries for destructive writes
        let (idx, queries): (Vec<_>, Vec<_>) = pipeline.prefetch().into_iter().unzip();
        let results = self.read_executor.execute(&queries)?;
        pipeline.store_prefetch(idx.into_iter().zip(results).collect());

        // Execute write queries and generate required read queries
        let (idx, writes): (Vec<_>, Vec<_>) = pipeline.get_writes().into_iter().unzip();
        let results = self.write_executor.execute(writes)?;
        let (idx, reads): (Vec<_>, Vec<_>) = pipeline
            .process_writes(idx.into_iter().zip(results).collect())
            .into_iter()
            .unzip();

        // Execute read queries created by write-queries
        let results = self.read_executor.execute(&reads)?;
        pipeline.store_reads(idx.into_iter().zip(results.into_iter()).collect());

        // Now execute all remaining reads
        let (idx, queries): (Vec<_>, Vec<_>) = pipeline.get_reads().into_iter().unzip();
        let results = self.read_executor.execute(&queries)?;
        pipeline.store_reads(idx.into_iter().zip(results).collect());

        // Consume pipeline into return value
        Ok(pipeline.consume())
    }

    /// Returns db name used in the executor.
    pub fn db_name(&self) -> String {
        self.write_executor.db_name.clone()
    }
}
