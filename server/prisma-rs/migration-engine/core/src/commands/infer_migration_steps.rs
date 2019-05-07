use crate::commands::command::MigrationCommand;
use migration_connector::steps::*;
use migration_connector::*;

pub struct InferMigrationStepsCommand {
    input: InferMigrationStepsInput,
}

impl MigrationCommand for InferMigrationStepsCommand {
    type Input = InferMigrationStepsInput;
    type Output = InferMigrationStepsOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(InferMigrationStepsCommand { input })
    }

    fn execute(&self) -> Self::Output {
        println!("{:?}", self.input);
        InferMigrationStepsOutput {
            steps: vec![],
            errors: vec![],
            warnings: vec![],
            general_errors: vec![],
        }
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct InferMigrationStepsInput {
    pub project_info: String,
    pub migration_id: String,
    pub data_model: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InferMigrationStepsOutput {
    pub steps: Vec<MigrationStep>,
    pub warnings: Vec<MigrationWarning>,
    pub errors: Vec<MigrationError>,
    pub general_errors: Vec<String>,
}
