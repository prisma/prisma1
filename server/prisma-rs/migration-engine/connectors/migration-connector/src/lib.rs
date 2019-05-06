pub mod steps;

use chrono::{DateTime, Utc};
use prisma_datamodel::Schema;
use std::sync::Arc;
pub use steps::MigrationStep;

#[macro_use]
extern crate serde_derive;

pub trait MigrationConnector {
    type DatabaseMigrationStep;

    fn migration_persistence(&self) -> Arc<MigrationPersistence>;

    fn database_steps_inferrer(&self) -> Arc<DatabaseMigrationStepsInferrer<Self::DatabaseMigrationStep>>;
    fn database_step_applier(&self) -> Arc<DatabaseMigrationStepApplier<Self::DatabaseMigrationStep>>;
    fn destructive_changes_checker(&self) -> Arc<DestructiveChangesChecker<Self::DatabaseMigrationStep>>;
}

pub trait DatabaseMigrationStepsInferrer<T> {
    fn infer(&self, previous: &Schema, next: &Schema, steps: Vec<MigrationStep>) -> Vec<T>;
}

pub trait DatabaseMigrationStepApplier<T> {
    fn apply(&self, step: T);
}

pub trait DestructiveChangesChecker<T> {
    fn check(&self, steps: Vec<T>) -> Vec<MigrationResult>;
}

pub enum MigrationResult {
    Error(MigrationWarning),
    Warning(MigrationError),
}

#[derive(Debug, Serialize)]
pub struct MigrationWarning {
    pub tpe: String,
    pub description: String,
    pub field: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct MigrationError {
    pub tpe: String,
    pub description: String,
    pub field: Option<String>,
}

pub trait MigrationPersistence {
    // returns the last successful Migration
    fn last(&self) -> Option<Migration>;

    // this power the listMigrations command
    // TODO: should this only return the successful ones? Or also the ones that were rolled back?
    fn load_all(&self) -> Vec<Migration>;

    // writes the migration to the Migration table
    fn create(&self, migration: Migration) -> Migration;

    // used by the MigrationApplier to write the progress of a Migration into the database
    fn update(&self, migration: Migration);
}

pub struct MigrationId {
    pub name: String,
    pub revision: u32,
}

pub struct Migration {
    pub id: MigrationId,
    pub status: MigrationStatus,
    pub applied: u32,
    pub rolled_back: u32,
    pub datamodel: Schema,
    pub datamodel_steps: Vec<String>,
    pub database_steps: Vec<String>,
    pub errors: Vec<String>,
    pub started_at: DateTime<Utc>,
    pub finished_at: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
pub enum MigrationStatus {
    Pending,
    InProgress,
    Success,
    RollingBack,
    RollbackSuccess,
    RollbackFailure,
}
