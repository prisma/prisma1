use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use chrono::*;
use migration_connector::*;

pub struct MigrationProgressCommand {
    input: MigrationProgressInput,
}

#[allow(unused)]
impl MigrationCommand for MigrationProgressCommand {
    type Input = MigrationProgressInput;
    type Output = MigrationProgressOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(MigrationProgressCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> Self::Output {
        let migration_persistence = engine.connector().migration_persistence();
        let migration = migration_persistence
            .by_name(&self.input.migration_id)
            .expect("Could not load migration from database.");
        MigrationProgressOutput {
            status: migration.status,
            steps: migration.datamodel_steps.len(),
            applied: migration.applied,
            rolled_back: migration.rolled_back,
            errors: migration.errors,
            started_at: migration.started_at,
            finished_at: migration.finished_at,
        }
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct MigrationProgressInput {
    pub project_info: String,
    pub migration_id: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MigrationProgressOutput {
    status: MigrationStatus,
    steps: usize,
    applied: usize,
    rolled_back: usize,
    errors: Vec<String>,
    started_at: DateTime<Utc>,
    finished_at: Option<DateTime<Utc>>,
}
