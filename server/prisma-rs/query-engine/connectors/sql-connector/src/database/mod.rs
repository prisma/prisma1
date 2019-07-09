mod mysql;
mod postgresql;
mod sqlite;

use crate::{query_builder::ManyRelatedRecordsQueryBuilder, Transactional};
pub use mysql::*;
pub use postgresql::*;
pub use sqlite::*;

pub trait SqlCapabilities {
    /// This we use to differentiate between databases with or without
    /// `ROW_NUMBER` function for related records pagination.
    type ManyRelatedRecordsBuilder: ManyRelatedRecordsQueryBuilder;
}

/// A wrapper for relational databases due to trait restrictions. Implements the
/// needed traits.
pub struct SqlDatabase<T>
where
    T: Transactional + SqlCapabilities,
{
    pub executor: T,
}

impl<T> SqlDatabase<T>
where
    T: Transactional + SqlCapabilities,
{
    pub fn new(executor: T) -> Self {
        Self { executor }
    }
}
