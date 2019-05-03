use chrono::{DateTime, Utc};
use prisma_datamodel::Schema;

pub mod steps;

#[macro_use]
extern crate serde_derive;

trait MigrationConnector {
    type DatabaseMigrationStep;

    fn migration_persistence(&self) -> MigrationPersistence;

    fn database_steps_inferrer(&self) -> DatabaseMigrationStepsInferrer<Self::DatabaseMigrationStep>;
    fn database_step_applier(&self) -> DatabaseMigrationStepApplier<Self::DatabaseMigrationStep>;
    fn destructive_changes_checker(&self) -> DestructiveChangesChecker<Self::DatabaseMigrationStep>;
}

trait DatabaseMigrationStepsInferrer<T> {
    fn infer(&self, previous: &Schema, current: &Schema) -> Vec<T>;
}

trait DatabaseMigrationStepApplier<T> {
    fn apply(&self, step: T);
}

trait DestructiveChangesChecker<T> {
    fn check(&self, steps: Vec<T>) -> Vec<MigrationResult>;
}

pub enum MigrationResult {
    Error(MigrationWarning),
    Warning(MigrationError),
}

pub struct MigrationWarning {
    pub tpe: String,
    pub description: String,
    pub field: Option<String>,
}

pub struct MigrationError {
    pub tpe: String,
    pub description: String,
    pub field: Option<String>,
}

trait MigrationPersistence {
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

#[derive(Debug)]
pub enum MigrationStatus {
    Pending,
    InProgress,
    Success,
    RollingBack,
    RollbackSuccess,
    RollbackFailure,
}
