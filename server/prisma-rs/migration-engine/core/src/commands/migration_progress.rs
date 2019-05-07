use crate::commands::command::MigrationCommand;
use chrono::*;
use migration_connector::*;

pub struct MigrationProgressCommand {
    input: MigrationProgressInput,
}

impl MigrationCommand for MigrationProgressCommand {
    type Input = MigrationProgressInput;
    type Output = MigrationProgressOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(MigrationProgressCommand { input })
    }

    fn execute(&self) -> Self::Output {
        println!("{:?}", self.input);
        MigrationProgressOutput {
            state: MigrationStatus::Pending,
            steps: 1,
            applied: 0,
            rolled_back: 0,
            errors: vec![],
            started_at: Utc::now(),
            finished_at: Utc::now(),
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
    state: MigrationStatus,
    steps: u32,
    applied: u32,
    rolled_back: u32,
    errors: Vec<String>,
    started_at: DateTime<Utc>,
    finished_at: DateTime<Utc>,
}
