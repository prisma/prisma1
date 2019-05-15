mod postgresql;
mod sqlite;

use crate::Transactional;
pub use postgresql::*;
pub use sqlite::*;

/// A wrapper for relational databases due to trait restrictions. Implements the
/// needed traits.
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
