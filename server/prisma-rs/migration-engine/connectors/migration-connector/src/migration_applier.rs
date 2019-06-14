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
        assert_eq!(migration.status, MigrationStatus::Pending); // what other states are valid here?
        let mut migration_updates = migration.update_params();
        migration_updates.status = MigrationStatus::MigrationInProgress;
        self.migration_persistence.update(&migration_updates);

        let apply_result = self.go_forward(&mut migration_updates, database_migration);

        match apply_result {
            Ok(()) => {
                migration_updates.mark_as_finished();
                self.migration_persistence.update(&migration_updates);
                Ok(())
            }
            Err(err) => {
                migration_updates.status = MigrationStatus::MigrationFailure;
                migration_updates.errors = vec![format!("{:?}", err)];
                self.migration_persistence.update(&migration_updates);
                Err(err)
            }
        }

    }

    fn unapply(&self, migration: &Migration, database_migration: &T) -> ConnectorResult<()> {
        assert_eq!(migration.status, MigrationStatus::MigrationSuccess); // what other states are valid here?
        let mut migration_updates = migration.update_params();
        migration_updates.status = MigrationStatus::RollingBack;
        self.migration_persistence.update(&migration_updates);

        let unapply_result = self.go_backward(&mut migration_updates, database_migration);

        match unapply_result {
            Ok(()) => {
                migration_updates.status = MigrationStatus::RollbackSuccess;
                self.migration_persistence.update(&migration_updates);
                Ok(())
            }
            Err(err) => {
                migration_updates.status = MigrationStatus::RollbackFailure;
                migration_updates.errors = vec![format!("{:?}", err)];
                self.migration_persistence.update(&migration_updates);
                Err(err)
            }
        }

    }
}

impl<T> MigrationApplierImpl<T> {
    fn go_forward(&self, migration_updates: &mut MigrationUpdateParams, database_migration: &T) -> ConnectorResult<()> {
        let mut step = 0;
        while self.step_applier.apply_step(&database_migration, step)? {
            step += 1;
            migration_updates.applied += 1;
            self.migration_persistence.update(&migration_updates);
        }
        Ok(())
    }

    fn go_backward(&self, migration_updates: &mut MigrationUpdateParams, database_migration: &T) -> ConnectorResult<()> {
        let mut step = 0;
        while self.step_applier.unapply_step(&database_migration, step)? {
            step += 1;
            migration_updates.rolled_back += 1;
            self.migration_persistence.update(&migration_updates);
        }
        Ok(())
    }
}