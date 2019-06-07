use super::MigrationStepsResultOutput;
use crate::commands::command::{CommandResult, MigrationCommand};
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct InferMigrationStepsCommand {
    input: InferMigrationStepsInput,
}

impl MigrationCommand for InferMigrationStepsCommand {
    type Input = InferMigrationStepsInput;
    type Output = MigrationStepsResultOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(InferMigrationStepsCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        let connector = engine.connector();
        let migration_persistence = connector.migration_persistence();
        let current_datamodel = migration_persistence.current_datamodel();
        let assumed_datamodel = engine
            .datamodel_calculator()
            .infer(&current_datamodel, &self.input.assume_to_be_applied);

        let next_datamodel = datamodel::parse(&self.input.data_model)?;

        let model_migration_steps = engine
            .datamodel_migration_steps_inferrer()
            .infer(&assumed_datamodel, &next_datamodel);

        let database_migration =
            connector
                .database_migration_inferrer()
                .infer(&assumed_datamodel, &next_datamodel, &model_migration_steps);

        let database_steps_json = connector
            .database_migration_step_applier()
            .render_steps_pretty(&database_migration);

        let returned_datamodel_steps = if self.input.is_watch_migration() {
            model_migration_steps
        } else {
            let mut steps = migration_persistence.load_all_datamodel_steps_from_all_current_watch_migrations();
            steps.append(&mut model_migration_steps.clone());
            steps
        };

        Ok(MigrationStepsResultOutput {
            datamodel: datamodel::render(&next_datamodel).unwrap(),
            datamodel_steps: returned_datamodel_steps,
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

impl IsWatchMigration for InferMigrationStepsInput {
    fn is_watch_migration(&self) -> bool {
        self.migration_id.starts_with("watch")
    }
}