mod database_migration_inferrer;
mod database_migration_step_applier;
mod destructive_changes_checker;
mod migration_applier;
mod migration_persistence;
mod error;

pub mod steps;

pub use database_migration_inferrer::*;
pub use database_migration_step_applier::*;
pub use destructive_changes_checker::*;
pub use migration_applier::*;
pub use migration_persistence::*;
pub use error::*;
use std::fmt::Debug;
use std::sync::Arc;
pub use steps::*;

#[macro_use]
extern crate serde_derive;

pub trait MigrationConnector: Send + Sync + 'static {
    type DatabaseMigration: DatabaseMigrationMarker + 'static;

    fn connector_type(&self) -> &'static str;

    fn initialize(&self) -> ConnectorResult<()>;

    fn reset(&self) -> ConnectorResult<()>;

    fn migration_persistence(&self) -> Arc<MigrationPersistence>;

    fn database_migration_inferrer(&self) -> Arc<DatabaseMigrationInferrer<Self::DatabaseMigration>>;
    fn database_migration_step_applier(&self) -> Arc<DatabaseMigrationStepApplier<Self::DatabaseMigration>>;
    fn destructive_changes_checker(&self) -> Arc<DestructiveChangesChecker<Self::DatabaseMigration>>;

    // TODO: figure out if this is the best way to do this or move to a better place/interface
    // this is placed here so i can use the associated type
    fn deserialize_database_migration(&self, json: serde_json::Value) -> Self::DatabaseMigration;

    fn migration_applier(&self) -> Box<MigrationApplier<Self::DatabaseMigration>> {
        let applier = MigrationApplierImpl {
            migration_persistence: self.migration_persistence(),
            step_applier: self.database_migration_step_applier(),
        };
        Box::new(applier)
    }
}

pub trait DatabaseMigrationMarker: Debug {
    fn serialize(&self) -> serde_json::Value;
}

pub type ConnectorResult<T> = Result<T, ConnectorError>;
