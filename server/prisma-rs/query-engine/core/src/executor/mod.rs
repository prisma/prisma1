//! A slightly more generic interface over executing read and write queries
#![warn(warnings)]

mod read;
mod write;

pub use read::ReadQueryExecutor;
pub use write::WriteQueryExecutor;

use crate::{Query, WriteQuery, WriteQueryResult, ReadQuery, ReadQueryResult};

/// A wrapper around QueryExecutor
pub struct Executor {
    pub read_exec: ReadQueryExecutor,
    pub write_exec: WriteQueryExecutor,
}

impl Executor {

    /// Can be given a list of both ReadQueries and WriteQueries
    ///
    /// Will execute WriteQueries first, then all ReadQueries, while preserving order.
    pub fn exec_all(&mut self, queries: Vec<Query>) {
        let (writes, reads) = Self::split_read_write(queries);
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