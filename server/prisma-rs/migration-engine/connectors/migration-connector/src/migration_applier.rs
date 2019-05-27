use crate::*;
use database_inspector::{SqlError}; // TODO: Replace by own error type for this crate.

pub trait MigrationApplier<T> {
    fn apply_steps(&self, migration: Migration, steps: Vec<T>, connector: &MigrationConnector<T>) -> Result<(), SqlError>;
}