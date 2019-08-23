use super::MigrationStepsResultOutput;
use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use datamodel::Datamodel;
use migration_connector::*;

pub struct CalculateDatabaseStepsCommand<'a> {
    input: &'a CalculateDatabaseStepsInput,
}

impl<'a> MigrationCommand<'a> for CalculateDatabaseStepsCommand<'a> {
    type Input = CalculateDatabaseStepsInput;
    type Output = MigrationStepsResultOutput;

    fn new(input: &'a Self::Input) -> Box<Self> {
        Box::new(CalculateDatabaseStepsCommand { input })
    }

    fn execute<C, D>(&self, engine: &MigrationEngine<C, D>) -> CommandResult<Self::Output>
    where
        C: MigrationConnector<DatabaseMigration = D>,
        D: DatabaseMigrationMarker + 'static,
    {
        debug!("{:?}", self.input);

        let connector = engine.connector();

        let assumed_datamodel = engine
            .datamodel_calculator()
            .infer(&Datamodel::empty(), &self.input.assume_to_be_applied);

        let next_datamodel = engine
            .datamodel_calculator()
            .infer(&assumed_datamodel, &self.input.steps_to_apply);

        let database_migration = connector.database_migration_inferrer().infer(
            &assumed_datamodel,
            &next_datamodel,
            &self.input.steps_to_apply,
        )?;

        let database_steps_json = connector
            .database_migration_step_applier()
            .render_steps_pretty(&database_migration)?;

        Ok(MigrationStepsResultOutput {
            datamodel: datamodel::render(&next_datamodel).unwrap(),
            datamodel_steps: self.input.steps_to_apply.clone(),
            database_steps: database_steps_json,
            errors: Vec::new(),
            warnings: Vec::new(),
            general_errors: Vec::new(),
        })
    }
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct CalculateDatabaseStepsInput {
    pub assume_to_be_applied: Vec<MigrationStep>,
    pub steps_to_apply: Vec<MigrationStep>,
}
