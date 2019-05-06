pub mod steps;

use chrono::{ DateTime, Utc };
use prisma_datamodel::Schema;
use std::str::FromStr;
use std::sync::Arc;
pub use steps::MigrationStep;

#[macro_use]
extern crate serde_derive;

pub trait MigrationConnector {
    type DatabaseMigrationStep;

    fn initialize(&self);

    fn reset(&self);

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

#[derive(Debug, PartialEq, Clone)]
pub struct MigrationId {
    pub name: String,
    pub revision: u32,
}

#[derive(Debug, PartialEq, Clone)]
pub struct Migration {
    pub id: MigrationId,
    pub status: MigrationStatus,
    pub applied: usize,
    pub rolled_back: usize,
    pub datamodel: Schema,
    pub datamodel_steps: Vec<String>,
    pub database_steps: Vec<String>,
    pub errors: Vec<String>,
    pub started_at: DateTime<Utc>,
    pub finished_at: Option<DateTime<Utc>>,
}

impl Migration {
    pub fn new(name: String) -> Migration {
        Migration {
            id: MigrationId {
                name: name,
                revision: 0,
            },
            status: MigrationStatus::Pending,
            applied: 0,
            rolled_back: 0,
            datamodel: Schema::empty(),
            datamodel_steps: Vec::new(),
            database_steps: Vec::new(),
            errors: Vec::new(),
            started_at: timestamp_without_nanos(),
            finished_at: None,
        }
    }
}

fn timestamp_without_nanos() -> DateTime<Utc> {
    let timestamp = Utc::now().timestamp_millis();
    let nsecs = ((timestamp % 1000) * 1_000_000) as u32;
    let secs = (timestamp / 1000) as i64;
    let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
    let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);
    datetime
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
