use crate::*;
use migration_connector::{Migration, MigrationApplier, MigrationStatus, MigrationStep, MigrationPersistence, DatabaseMigrationStepApplier};
use prisma_query::{error::Error as SqlError, transaction::Connectional};

#[allow(unused, dead_code)]
pub struct SqlMigrationApplier {
    pub schema_name: String
}

#[allow(unused, dead_code)]
impl MigrationApplier<SqlMigrationStep> for SqlMigrationApplier {
    type ErrorType = SqlError;
    type ConnectionType = &'static mut Connection;

    fn apply_steps(&mut self, connection: &mut Connection, migration: Migration, steps: Vec<SqlMigrationStep>) -> Result<(), SqlError> {

        let persistence = SqlMigrationPersistence::new();
        let applier = SqlDatabaseStepApplier::new(&self.schema_name);

        // todo: refactor those procedural updates into proper domain methods on the Migration struct
        assert_eq!(migration.status, MigrationStatus::Pending); // what other states are valid here?

        let mut migration_updates = migration.update_params();
        migration_updates.status = MigrationStatus::InProgress;
        persistence.update(connection, &migration_updates);

        for step in steps {
            applier.apply(connection, step)?;
            migration_updates.applied = migration_updates.applied + 1;
            persistence.update(connection, &migration_updates)?;
        }

        migration_updates.status = MigrationStatus::Success;
        migration_updates.finished_at = Some(Migration::timestamp_without_nanos());
        persistence.update(connection, &migration_updates)?;

        Ok(())
    }
}
