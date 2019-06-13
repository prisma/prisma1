use crate::*;
use std::sync::Arc;

pub trait MigrationApplier<T> {
    fn apply(&self, migration: &Migration, database_migration: &T) -> ConnectorResult<()>;

    fn unapply(&self, migration: &Migration, database_migration: &T) -> ConnectorResult<()>;
}

#[allow(unused, dead_code)]
pub struct MigrationApplierImpl<T> {
    pub migration_persistence: Arc<MigrationPersistence>,
    pub step_applier: Arc<DatabaseMigrationStepApplier<T>>,
}

#[allow(unused, dead_code)]
impl<T> MigrationApplier<T> for MigrationApplierImpl<T> {
    fn apply(&self, migration: &Migration, database_migration: &T) -> ConnectorResult<()> {
        // todo: refactor those procedural updates into proper domain methods on the Migration struct
        assert_eq!(migration.status, MigrationStatus::Pending); // what other states are valid here?

        let mut migration_updates = migration.update_params();
        migration_updates.status = MigrationStatus::InProgress;
        self.migration_persistence.update(&migration_updates);

        let mut step = 0;
        while (self.step_applier.apply_step(&database_migration, step)?) {
            step = step + 1;
            migration_updates.applied = migration_updates.applied + 1;
            self.migration_persistence.update(&migration_updates);
        }

        migration_updates.status = MigrationStatus::Success;
        migration_updates.finished_at = Some(Migration::timestamp_without_nanos());
        self.migration_persistence.update(&migration_updates);
        Ok(())
    }

    fn unapply(&self, migration: &Migration, database_migration: &T) -> ConnectorResult<()> {
        assert_eq!(migration.status, MigrationStatus::Success); // what other states are valid here?
        let mut migration_updates = migration.update_params();
        migration_updates.status = MigrationStatus::RollingBack;
        self.migration_persistence.update(&migration_updates);

        let mut step = 0;
        while (self.step_applier.unapply_step(&database_migration, step)?) {
            step = step + 1;
            migration_updates.rolled_back = migration_updates.rolled_back + 1;
            self.migration_persistence.update(&migration_updates);
        }

        migration_updates.status = MigrationStatus::RollbackSuccess;
        self.migration_persistence.update(&migration_updates);
        Ok(())
    }
}
