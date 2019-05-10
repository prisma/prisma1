use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct ApplyMigrationCommand {
    input: ApplyMigrationInput,
}

impl MigrationCommand for ApplyMigrationCommand {
    type Input = ApplyMigrationInput;
    type Output = ApplyMigrationOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ApplyMigrationCommand { input })
    }

    fn execute(&self, engine: Box<MigrationEngine>) -> Self::Output {
        println!("{:?}", self.input);
        ApplyMigrationOutput {
            steps: Vec::new(),
            errors: Vec::new(),
            warnings: Vec::new(),
            general_errors: Vec::new(),
        }
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApplyMigrationInput {
    pub project_info: String,
    pub migration_id: String,
    pub steps: Vec<MigrationStep>,
    pub force: bool,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ApplyMigrationOutput {
    pub steps: Vec<MigrationStep>,
    pub warnings: Vec<MigrationWarning>,
    pub errors: Vec<MigrationError>,
    pub general_errors: Vec<String>,
}
