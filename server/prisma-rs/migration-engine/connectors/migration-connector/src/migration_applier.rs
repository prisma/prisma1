use crate::*;
use std::sync::Arc;

pub trait MigrationApplier<T> {
    fn apply_steps(&mut self, migration: Migration, steps: Vec<T>);
}