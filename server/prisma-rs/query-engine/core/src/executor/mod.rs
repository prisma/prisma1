//! A slightly more generic interface over executing read and write queries

mod pipeline;
mod read;
mod write;

use self::pipeline::*;

pub use read::ReadQueryExecutor;
pub use write::WriteQueryExecutor;

use crate::{CoreResult, Query, ReadQueryResult};

/// A wrapper around QueryExecutor
pub struct Executor {
    pub read_exec: ReadQueryExecutor,
    pub write_exec: WriteQueryExecutor,
}

impl Executor {
    /// Can be given a list of both ReadQueries and WriteQueries
    ///
    /// Will execute WriteQueries first, then all ReadQueries, while preserving order.
    pub fn exec_all(&self, queries: Vec<Query>) -> CoreResult<Vec<ReadQueryResult>> {
        // Give all queries to the pipeline module
        let mut pipeline = QueryPipeline::from(queries);

        // Execute prefetch queries for destructive writes
        let (idx, queries): (Vec<_>, Vec<_>) = pipeline.prefetch().into_iter().unzip();
        let results = self.read_exec.execute(&queries)?;
        pipeline.store_prefetch(idx.into_iter().zip(results).collect());

        // Execute write queries and generate required read queries
        let (idx, writes): (Vec<_>, Vec<_>) = pipeline.get_writes().into_iter().unzip();
        let results = self.write_exec.execute(writes)?;
        let (idx, reads): (Vec<_>, Vec<_>) = pipeline
            .process_writes(idx.into_iter().zip(results).collect())
            .into_iter()
            .unzip();

        // Execute read queries created by write-queries
        let results = self.read_exec.execute(&reads)?;
        pipeline.store_reads(idx.into_iter().zip(results.into_iter()).collect());

        // Now execute all remaining reads
        let (idx, queries): (Vec<_>, Vec<_>) = pipeline.get_reads().into_iter().unzip();
        let results = self.read_exec.execute(&queries)?;
        pipeline.store_reads(idx.into_iter().zip(results).collect());

        // Consume pipeline into return value
        Ok(pipeline.consume())
    }

    /// Returns db name used in the executor.
    pub fn db_name(&self) -> String {
        self.write_exec.db_name.clone()
    }
}
