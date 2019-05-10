use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use datamodel::dml::*;
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

    fn execute(&self, engine: Box<MigrationEngine>) -> Self::Output {
        println!("{:?}", self.input);
        let connector = engine.connector();
        let current_data_model = connector
            .migration_persistence()
            .last()
            .map(|m| m.datamodel)
            .unwrap_or(Schema::empty());

        let next_data_model = engine.parse_datamodel(&self.input.data_model);

        let model_migration_steps = engine
            .datamodel_migration_steps_inferrer()
            .infer(current_data_model.clone(), next_data_model.clone());

        let database_migration_steps = connector.database_steps_inferrer().infer(
            &current_data_model,
            &next_data_model,
            model_migration_steps.clone(),
        );

        InferMigrationStepsOutput {
            steps: model_migration_steps,
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
