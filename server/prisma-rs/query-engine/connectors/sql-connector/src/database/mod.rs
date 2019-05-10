mod postgresql;
mod sqlite;

use crate::Transactional;
pub use postgresql::*;
pub use sqlite::*;

/// A common interface for relational SQL databases.
pub struct SqlDatabase<T>
where
    T: Transactional,
{
    pub executor: T,
}

impl<T> SqlDatabase<T>
where
    T: Transactional,
{
    pub fn new(executor: T) -> Self {
        Self { executor }
    }
}
