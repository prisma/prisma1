use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use migration_connector::steps::*;
use migration_connector::*;

pub struct ListMigrationStepsCommand {
    input: ListMigrationStepsInput,
}

#[allow(unused)]
impl MigrationCommand for ListMigrationStepsCommand {
    type Input = ListMigrationStepsInput;
    type Output = Vec<ListMigrationStepsOutput>;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ListMigrationStepsCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> Self::Output {
        println!("{:?}", self.input);
        let migration_persistence = engine.connector().migration_persistence();
        migration_persistence
            .load_all()
            .into_iter()
            .map(|mig| ListMigrationStepsOutput {
                id: mig.name,
                steps: mig.datamodel_steps,
                status: mig.status,
            })
            .collect()
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
