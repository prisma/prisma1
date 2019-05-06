use crate::commands::command::MigrationCommand;
use migration_connector::steps::*;
use migration_connector::*;

pub struct ListMigrationStepsCommand {
    input: ListMigrationStepsInput,
}

impl MigrationCommand for ListMigrationStepsCommand {
    type Input = ListMigrationStepsInput;
    type Output = Vec<ListMigrationStepsOutput>;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ListMigrationStepsCommand { input })
    }

    fn execute(&self) -> Self::Output {
        println!("{:?}", self.input);
        vec![]
    }
}


#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ListMigrationStepsInput {
    pub project_info: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ListMigrationStepsOutput {
    pub id: String,
    pub steps: Vec<MigrationStep>,
    pub status: MigrationStatus,
}