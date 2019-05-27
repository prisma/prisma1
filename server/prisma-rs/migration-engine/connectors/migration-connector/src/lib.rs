mod migration_applier;
pub mod steps;

use chrono::{DateTime, Utc};
use database_inspector::SqlError;
use datamodel::Datamodel;
pub use migration_applier::*;
use serde::Serialize;
use std::fmt::Debug;
pub use steps::MigrationStep; // TODO: Replace by own error type for this crate.

#[macro_use]
extern crate serde_derive;

// TODO: Not sure if generic here is a good idea.
pub trait MigrationConnector<InternalStepType: DatabaseMigrationStepExt> {
    fn initialize(&self) -> Result<(), SqlError>;
    fn reset(&self) -> Result<(), SqlError>;

    fn migration_persistence(&self) -> &MigrationPersistence;
    fn database_steps_inferrer(&self) -> &DatabaseMigrationStepsInferrer<InternalStepType>;
    fn database_step_applier(&self) -> &DatabaseMigrationStepApplier<InternalStepType>;
    fn destructive_changes_checker(&self) -> &DestructiveChangesChecker<InternalStepType>;

    fn migration_applier(&self) -> &MigrationApplier<InternalStepType>;
}

pub trait DatabaseMigrationStepExt: Debug + Serialize {}

pub trait DatabaseMigrationStepsInferrer<T> {
    fn infer(&self, previous: &Datamodel, next: &Datamodel, steps: Vec<MigrationStep>) -> Result<Vec<T>, SqlError>;
}

pub trait DatabaseMigrationStepApplier<T> {
    fn apply(&self, step: T) -> Result<(), SqlError>;
}

pub trait DestructiveChangesChecker<T> {
    fn check(&self, steps: Vec<T>) -> Result<Vec<MigrationResult>, SqlError>;
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
    fn last(&self) -> Result<Migration, SqlError>;

    fn by_name(&self, name: &str) -> Result<Migration, SqlError>;

    // this power the listMigrations command
    fn load_all(&self) -> Result<Vec<Migration>, SqlError>;

    // writes the migration to the Migration table
    fn create(&self, migration: Migration) -> Result<Migration, SqlError>;

    // used by the MigrationApplier to write the progress of a Migration into the database
    fn update(&self, params: &MigrationUpdateParams) -> Result<Migration, SqlError>;
}

#[derive(Debug, PartialEq, Clone)]
pub struct Migration {
    pub name: String,
    pub revision: usize,
    pub status: MigrationStatus,
    pub applied: usize,
    pub rolled_back: usize,
    pub datamodel: Datamodel,
    pub datamodel_steps: Vec<MigrationStep>,
    pub database_steps: String,
    pub errors: Vec<String>,
    pub started_at: DateTime<Utc>,
    pub finished_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Clone)]
pub struct MigrationUpdateParams {
    pub name: String,
    pub revision: usize,
    pub status: MigrationStatus,
    pub applied: usize,
    pub rolled_back: usize,
    pub errors: Vec<String>,
    pub finished_at: Option<DateTime<Utc>>,
}

impl Migration {
    pub fn new(name: String) -> Migration {
        Migration {
            name: name,
            revision: 0,
            status: MigrationStatus::Pending,
            applied: 0,
            rolled_back: 0,
            datamodel: Datamodel::empty(),
            datamodel_steps: Vec::new(),
            database_steps: "[]".to_string(),
            errors: Vec::new(),
            started_at: Self::timestamp_without_nanos(),
            finished_at: None,
        }
    }

    pub fn update_params(&self) -> MigrationUpdateParams {
        MigrationUpdateParams {
            name: self.name.clone(),
            revision: self.revision.clone(),
            status: self.status.clone(),
            applied: self.applied,
            rolled_back: self.rolled_back,
            errors: self.errors.clone(),
            finished_at: self.finished_at.clone(),
        }
    }

    // SQLite does not store nano precision. Therefore we cut it so we can assert equality in our tests.
    pub fn timestamp_without_nanos() -> DateTime<Utc> {
        let timestamp = Utc::now().timestamp_millis();
        let nsecs = ((timestamp % 1000) * 1_000_000) as u32;
        let secs = (timestamp / 1000) as i64;
        let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
        let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);
        datetime
    }
}

#[derive(Debug, Serialize, PartialEq, Clone)]
pub enum MigrationStatus {
    Pending,
    InProgress,
    Success,
    RollingBack,
    RollbackSuccess,
    RollbackFailure,
}

impl MigrationStatus {
    pub fn code(&self) -> &str {
        match self {
            MigrationStatus::Pending => "Pending",
            MigrationStatus::InProgress => "InProgress",
            MigrationStatus::Success => "Success",
            MigrationStatus::RollingBack => "RollingBack",
            MigrationStatus::RollbackSuccess => "RollbackSuccess",
            MigrationStatus::RollbackFailure => "RollbackFailure",
        }
    }

    pub fn from_str(s: String) -> MigrationStatus {
        match s.as_ref() {
            "Pending" => MigrationStatus::Pending,
            "InProgress" => MigrationStatus::InProgress,
            "Success" => MigrationStatus::Success,
            "RollingBack" => MigrationStatus::RollingBack,
            "RollbackSuccess" => MigrationStatus::RollbackSuccess,
            "RollbackFailure" => MigrationStatus::RollbackFailure,
            _ => panic!("MigrationStatus {:?} is not known", s),
        }
    }
}
