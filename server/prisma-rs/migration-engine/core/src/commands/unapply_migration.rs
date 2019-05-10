use super::list_migrations::ListMigrationStepsOutput;
use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct UnapplyMigrationCommand {
    input: UnapplyMigrationInput,
}

impl MigrationCommand for UnapplyMigrationCommand {
    type Input = UnapplyMigrationInput;
    type Output = UnapplyMigrationOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(UnapplyMigrationCommand { input })
    }

    fn execute(&self, engine: Box<MigrationEngine>) -> Self::Output {
        println!("{:?}", self.input);
        UnapplyMigrationOutput {
            rolled_back: ListMigrationStepsOutput {
                id: "foo".to_string(),
                steps: Vec::new(),
                status: MigrationStatus::Pending,
            },
            active: ListMigrationStepsOutput {
                id: "bar".to_string(),
                steps: Vec::new(),
                status: MigrationStatus::Pending,
            },
        }
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UnapplyMigrationInput {
    pub project_info: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnapplyMigrationOutput {
    pub rolled_back: ListMigrationStepsOutput,
    pub active: ListMigrationStepsOutput,
}
