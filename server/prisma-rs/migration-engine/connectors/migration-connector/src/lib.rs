mod migration_applier;
pub mod steps;

use chrono::{DateTime, Utc};
use datamodel::Datamodel;
pub use migration_applier::*;
use serde::Serialize;
use std::fmt::Debug;
use std::sync::Arc;
pub use steps::MigrationStep;
use database_inspector::DatabaseInspector;

#[macro_use]
extern crate serde_derive;

pub trait MigrationConnector {
    type DatabaseMigrationStep: DatabaseMigrationStepExt + 'static;

    fn initialize(&self);

    fn reset(&self);

    fn migration_persistence(&self) -> Arc<MigrationPersistence>;

    fn database_steps_inferrer(&self) -> Arc<DatabaseMigrationStepsInferrer<Self::DatabaseMigrationStep>>;
    fn database_step_applier(&self) -> Arc<DatabaseMigrationStepApplier<Self::DatabaseMigrationStep>>;
    fn destructive_changes_checker(&self) -> Arc<DestructiveChangesChecker<Self::DatabaseMigrationStep>>;

    fn migration_applier(&self) -> Box<MigrationApplier<Self::DatabaseMigrationStep>> {
        let applier = MigrationApplierImpl {
            migration_persistence: self.migration_persistence(),
            step_applier: self.database_step_applier(),
        };
        Box::new(applier)
    }

    fn database_inspector(&self) -> Box<DatabaseInspector> {
        DatabaseInspector::empty()
    }
}

pub trait DatabaseMigrationStepExt: Debug + Serialize {}

pub trait DatabaseMigrationStepsInferrer<T> {
    fn infer(&self, previous: &Datamodel, next: &Datamodel, steps: Vec<MigrationStep>) -> Vec<T>;
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

    fn by_name(&self, name: &str) -> Option<Migration>;

    // this power the listMigrations command
    fn load_all(&self) -> Vec<Migration>;

    // writes the migration to the Migration table
    fn create(&self, migration: Migration) -> Migration;

    // used by the MigrationApplier to write the progress of a Migration into the database
    fn update(&self, params: &MigrationUpdateParams);
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
