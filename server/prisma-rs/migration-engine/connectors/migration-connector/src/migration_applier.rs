use crate::*;
use std::sync::Arc;

pub trait MigrationApplier<T> {
    fn apply_steps(&self, migration: Migration, steps: Vec<T>);
}