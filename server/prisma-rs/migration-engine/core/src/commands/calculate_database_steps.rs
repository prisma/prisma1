use super::MigrationStepsResultOutput;
use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use datamodel::dml::Datamodel;
use migration_connector::*;

pub struct CalculateDatabaseStepsCommand {
    input: CalculateDatabaseStepsInput,
}

impl MigrationCommand for CalculateDatabaseStepsCommand {
    type Input = CalculateDatabaseStepsInput;
    type Output = MigrationStepsResultOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(CalculateDatabaseStepsCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> Self::Output {
        println!("{:?}", self.input);

        let connector = engine.connector();

        let current_datamodel = connector
            .migration_persistence()
            .last()
            .map(|m| m.datamodel)
            .unwrap_or(Datamodel::empty());

        let assumed_datamodel = engine
            .datamodel_calculator()
            .infer(&current_datamodel, &self.input.assume_to_be_applied);

        let next_datamodel = engine
            .datamodel_calculator()
            .infer(&assumed_datamodel, &self.input.steps_to_apply);

        let database_migration_steps =
            connector
                .database_steps_inferrer()
                .infer(&assumed_datamodel, &next_datamodel, &self.input.steps_to_apply);

        let database_steps_json = connector
            .database_step_applier()
            .render_steps(&database_migration_steps);

        MigrationStepsResultOutput {
            datamodel_steps: self.input.steps_to_apply.clone(),
            database_steps: database_steps_json,
            errors: Vec::new(),
            warnings: Vec::new(),
            general_errors: Vec::new(),
        }
    }
}

#[derive(Deserialize, Debug)]
pub struct CalculateDatabaseStepsInput {
    pub project_info: String,
    pub assume_to_be_applied: Vec<MigrationStep>,
    pub steps_to_apply: Vec<MigrationStep>,
}
