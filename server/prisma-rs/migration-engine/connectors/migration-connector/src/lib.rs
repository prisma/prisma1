mod migration_applier;
pub mod steps;

use chrono::{DateTime, Utc};
use datamodel::Schema;
pub use migration_applier::*;
use serde::Serialize;
use std::fmt::Debug;
pub use steps::MigrationStep;
use database_inspector::{IntrospectionResult as IntrospectionResultBase, IntrospectionConnector};

#[macro_use]
extern crate serde_derive;

pub trait MigrationConnector {
    // TODO: Most of these associated types are dynamic
    // because of the result error type. It would be smart to introduce a
    // custom but fixed Error type for this crate.
    type DatabaseMigrationStep: DatabaseMigrationStepExt + 'static;
    type IntrospectionResult: IntrospectionResultBase + 'static;
    type IntrospectionConnector: IntrospectionConnector<Self::IntrospectionResult> + 'static;
    type MigrationPersistenceType: MigrationPersistence + 'static;
    type MigrationApplierType: MigrationApplier<Self::DatabaseMigrationStep> + 'static;
    type DatabaseMigrationStepsApplierType: DatabaseMigrationStepApplier<Self::DatabaseMigrationStep> + 'static;
    type DatabaseMigrationStepsInferrerType: DatabaseMigrationStepsInferrer<Self::DatabaseMigrationStep> + 'static;
    type DatabaseDestructiveChangesCheckerType: DestructiveChangesChecker<Self::DatabaseMigrationStep> + 'static;

    fn initialize(&self);

    fn reset(&self);

    fn migration_persistence(&self) -> Box<Self::MigrationPersistenceType>;
    fn database_inspector(&self) -> Box<Self::IntrospectionConnector>;
    fn database_steps_inferrer(&self) -> Box<Self::DatabaseMigrationStepsInferrerType>;
    fn database_step_applier(&self) -> Box<Self::DatabaseMigrationStepsApplierType>;
    fn destructive_changes_checker(&self) -> Box<Self::DatabaseDestructiveChangesCheckerType>;

    fn migration_applier(&self) -> Box<Self::MigrationApplierType>;
    
    /* {
        let applier = MigrationApplierImpl {
            migration_persistence: self.migration_persistence(),
            step_applier: self.database_step_applier(),
        };
        Box::new(applier)
    }*/
}

pub trait DatabaseMigrationStepExt: Debug + Serialize {}

pub trait DatabaseMigrationStepsInferrer<T> {
    type DatabaseSchemaType;

    fn infer(&self, 
    previous: &Schema, 
    next: &Schema, 
    previous_database: &Self::DatabaseSchemaType, 
    next_database: &Self::DatabaseSchemaType, 
    steps: Vec<MigrationStep>) -> Vec<T>;
}

pub trait DatabaseMigrationStepApplier<T> {
    type ErrorType;
    fn apply(&self, step: T) -> Result<(), Self::ErrorType>;
}

pub trait DestructiveChangesChecker<T> {
    type ErrorType;
    fn check(&self, steps: Vec<T>) -> Result<Vec<MigrationResult>, Self::ErrorType>;
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
    type ErrorType;

    // returns the last successful Migration
    fn last(&self) -> Result<Migration, Self::ErrorType>;

    fn by_name(&self, name: &str) -> Result<Migration, Self::ErrorType>;

    // this power the listMigrations command
    fn load_all(&self) -> Result<Vec<Migration>, Self::ErrorType>;

    // writes the migration to the Migration table
    fn create(&self, migration: Migration) -> Result<Migration, Self::ErrorType>;

    // used by the MigrationApplier to write the progress of a Migration into the database
    fn update(&self, params: &MigrationUpdateParams) -> Result<Migration, Self::ErrorType>;
}

#[derive(Debug, PartialEq, Clone)]
pub struct Migration {
    pub name: String,
    pub revision: usize,
    pub status: MigrationStatus,
    pub applied: usize,
    pub rolled_back: usize,
    pub datamodel: Schema,
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
            datamodel: Schema::empty(),
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
