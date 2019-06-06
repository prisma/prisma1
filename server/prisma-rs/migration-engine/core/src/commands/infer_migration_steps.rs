use crate::commands::command::{MigrationCommand, CommandResult};
use crate::migration_engine::MigrationEngine;
use datamodel::Datamodel;
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

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        let connector = engine.connector();
        let current_data_model = if self.input.assume_to_be_applied.is_empty() {
            connector.migration_persistence().current_datamodel()
        } else {
            engine
                .datamodel_calculator()
                .infer(&Datamodel::empty(), &self.input.assume_to_be_applied)
        };

        let next_data_model = engine.parse_datamodel(&self.input.data_model);

        let model_migration_steps = engine
            .datamodel_migration_steps_inferrer()
            .infer(&current_data_model, &next_data_model);

        let database_migration = connector.database_migration_inferrer().infer(
            &current_data_model,
            &next_data_model,
            &model_migration_steps,
        );

        let database_steps_json = connector
            .database_migration_step_applier()
            .render_steps_pretty(&database_migration);

        Ok(InferMigrationStepsOutput {
            datamodel_steps: model_migration_steps,
            database_steps: database_steps_json,
            errors: vec![],
            warnings: vec![],
            general_errors: vec![],
        })
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct InferMigrationStepsInput {
    pub project_info: String,
    pub migration_id: String,
    pub data_model: String,
    pub assume_to_be_applied: Vec<MigrationStep>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InferMigrationStepsOutput {
    pub datamodel_steps: Vec<MigrationStep>,
    pub database_steps: serde_json::Value,
    pub warnings: Vec<MigrationWarning>,
    pub errors: Vec<MigrationError>,
    pub general_errors: Vec<String>,
}
