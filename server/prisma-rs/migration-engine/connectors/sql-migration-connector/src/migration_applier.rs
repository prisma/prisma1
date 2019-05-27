use crate::*;
use migration_connector::{Migration, MigrationConnector, MigrationApplier, MigrationStatus, MigrationStep, MigrationPersistence, DatabaseMigrationStepApplier};
use prisma_query::{error::Error as SqlError};

pub struct SqlMigrationApplier {
    schema_name: String,
}

impl SqlMigrationApplier {
    pub fn new(schema_name: &str) -> SqlMigrationApplier{
        SqlMigrationApplier {
            schema_name: String::from(schema_name),
        }
    }
}

impl MigrationApplier<SqlMigrationStep> for SqlMigrationApplier {

    fn apply_steps(&self, migration: Migration, steps: Vec<SqlMigrationStep>, connector: &MigrationConnector<SqlMigrationStep>) -> Result<(), SqlError> {

        let persistence = connector.migration_persistence();
        let applier = connector.database_step_applier();

        // todo: refactor those procedural updates into proper domain methods on the Migration struct
        assert_eq!(migration.status, MigrationStatus::Pending); // what other states are valid here?

        let mut migration_updates = migration.update_params();
        migration_updates.status = MigrationStatus::InProgress;
        persistence.update(&migration_updates);

        dbg!("Migration Steps");
        dbg!(&steps);

        for step in steps {
            applier.apply(step)?;
            migration_updates.applied = migration_updates.applied + 1;
            persistence.update(&migration_updates)?;
        }

        migration_updates.status = MigrationStatus::Success;
        migration_updates.finished_at = Some(Migration::timestamp_without_nanos());
        persistence.update(&migration_updates)?;

        Ok(())
    }
}
