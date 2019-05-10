use crate::*;
use std::sync::Arc;

pub trait MigrationApplier<T> {
    fn apply_steps(&self, steps: Vec<T>);
}

#[allow(unused, dead_code)]
pub struct MigrationApplierImpl<T> {
    pub migration_persistence: Arc<MigrationPersistence>,
    pub step_applier: Arc<DatabaseMigrationStepApplier<T>>,
}

#[allow(unused, dead_code)]
impl<T> MigrationApplier<T> for MigrationApplierImpl<T> {
    fn apply_steps(&self, steps: Vec<T>) {}
}
