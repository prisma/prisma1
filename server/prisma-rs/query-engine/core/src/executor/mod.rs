//! A slightly more generic interface over executing read and write queries
#![warn(warnings)]

mod read;
mod write;

pub use read::ReadQueryExecutor;
pub use write::WriteQueryExecutor;

use crate::{CoreResult, Query, WriteQuery, WriteQueryResult, ReadQuery, ReadQueryResult};

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
        let (writes, reads) = Self::split_read_write(queries);


        // FIXME: This is not how you do write-processing
        let reads: Vec<ReadQuery> = reads.into_iter().filter_map(|q| q).collect();

        self.read_exec.execute(reads.as_slice())
    }

    fn split_read_write(queries: Vec<Query>) -> (Vec<WriteQuery>, Vec<Option<ReadQuery>>) {
        queries
            .into_iter()
            .fold(
                (vec![], vec![]),
                |(mut w, mut r), query| {
                    match query {
                        Query::Write(q) => {
                            w.push(q); // Push WriteQuery
                            r.push(None); // Push Read placeholder
                        },
                        Query::Read(q) => r.push(Some(q)),
                    }

                    (w, r)
                })
    }
}