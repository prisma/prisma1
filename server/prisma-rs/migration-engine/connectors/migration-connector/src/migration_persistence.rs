use crate::steps::*;
use chrono::{DateTime, Utc};
use datamodel::Datamodel;

pub trait MigrationPersistence {
    fn init(&self);
    fn reset(&self);

    // returns the currently active Datamodel
    fn current_datamodel(&self) -> Datamodel {
        self.last().map(|m| m.datamodel).unwrap_or(Datamodel::empty())
    }

    fn last_non_watch_datamodel(&self) -> Datamodel {
        let mut all_migrations = self.load_all();
        all_migrations.reverse();
        all_migrations.into_iter().find(|m|!m.is_watch_migration()).map(|m|m.datamodel).unwrap_or(Datamodel::empty())
    }

    // returns the last successful Migration
    fn last(&self) -> Option<Migration>;

    fn by_name(&self, name: &str) -> Option<Migration>;

    // this power the listMigrations command
    fn load_all(&self) -> Vec<Migration>;

    // loads all current trailing watch migrations from Migration Event Log
    fn load_current_watch_migrations(&self) -> Vec<Migration> {
        let mut all_migrations = self.load_all();
        let mut result = Vec::new();
        // start to take all migrations from the back until we hit a migration that is not watch
        all_migrations.reverse();
        for migration in all_migrations {
            if migration.is_watch_migration() {
                result.push(migration);
            } else {
                break;
            }
        }
        // reverse the result so the migrations are in the right order again
        result.reverse();
        result
    }

    fn load_all_datamodel_steps_from_all_current_watch_migrations(&self) -> Vec<MigrationStep> {
        let all_watch_migrations = self.load_current_watch_migrations();
        let mut all_steps_from_all_watch_migrations = Vec::new();
        for mut migration in all_watch_migrations.into_iter() {
            all_steps_from_all_watch_migrations.append(&mut migration.datamodel_steps);
        }
        all_steps_from_all_watch_migrations
    }

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
    pub database_migration: serde_json::Value,
    pub errors: Vec<String>,
    pub started_at: DateTime<Utc>,
    pub finished_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Clone)]
pub struct MigrationUpdateParams {
    pub name: String,
    pub new_name: String,
    pub revision: usize,
    pub status: MigrationStatus,
    pub applied: usize,
    pub rolled_back: usize,
    pub errors: Vec<String>,
    pub finished_at: Option<DateTime<Utc>>,
}

pub trait IsWatchMigration {
    fn is_watch_migration(&self) -> bool;
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
            database_migration: serde_json::to_value("{}").unwrap(),
            errors: Vec::new(),
            started_at: Self::timestamp_without_nanos(),
            finished_at: None,
        }
    }

    pub fn update_params(&self) -> MigrationUpdateParams {
        MigrationUpdateParams {
            name: self.name.clone(),
            new_name: self.name.clone(),
            revision: self.revision.clone(),
            status: self.status.clone(),
            applied: self.applied,
            rolled_back: self.rolled_back,
            errors: self.errors.clone(),
            finished_at: self.finished_at.clone(),
        }
    }

    pub fn mark_as_finished(&mut self) {
        self.status = MigrationStatus::Success;
        self.finished_at = Some(Self::timestamp_without_nanos());
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

impl IsWatchMigration for Migration {
    fn is_watch_migration(&self) -> bool {
        self.name.starts_with("watch")
    }
}

#[derive(Debug, Serialize, PartialEq, Clone, Copy)]
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


pub struct EmptyMigrationPersistence{}
impl MigrationPersistence for EmptyMigrationPersistence {
    fn init(&self) {}

    fn reset(&self) {}

    fn last(&self) -> Option<Migration> {
        None
    }

    fn by_name(&self, _name: &str) -> Option<Migration> {
        None
    }

    fn load_all(&self) -> Vec<Migration> {
        Vec::new()
    }

    fn create(&self, _migration: Migration) -> Migration {
        unimplemented!("Not allowed on a EmptyMigrationPersistence")
    }

    fn update(&self, _params: &MigrationUpdateParams) {
        unimplemented!("Not allowed on a EmptyMigrationPersistence")
    }
}