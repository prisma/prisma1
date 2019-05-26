use crate::*;
use migration_connector::{Migration, MigrationApplier, MigrationStatus, MigrationStep, MigrationPersistence, DatabaseMigrationStepApplier};
use prisma_query::transaction::Connectional;

#[allow(unused, dead_code)]
pub struct SqlMigrationApplier<'a> {
    connection: &'a mut Connection,
    schema_name: String
}

#[allow(unused, dead_code)]
impl<'a> MigrationApplier<SqlMigrationStep> for SqlMigrationApplier<'a> {
    fn apply_steps(&mut self, migration: Migration, steps: Vec<SqlMigrationStep>) {
        let mut persistence = SqlMigrationPersistence::new(self.connection);
        let applier = SqlDatabaseStepApplier::new(self.connection, &self.schema_name);

        // todo: refactor those procedural updates into proper domain methods on the Migration struct
        assert_eq!(migration.status, MigrationStatus::Pending); // what other states are valid here?

        let mut migration_updates = migration.update_params();
        migration_updates.status = MigrationStatus::InProgress;
        persistence.update(&migration_updates);

        for step in steps {
            applier.apply(step);
            migration_updates.applied = migration_updates.applied + 1;
            persistence.update(&migration_updates);
        }

        migration_updates.status = MigrationStatus::Success;
        migration_updates.finished_at = Some(Migration::timestamp_without_nanos());
        persistence.update(&migration_updates);
    }
}
