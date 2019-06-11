use crate::commands::command::{CommandResult, MigrationCommand, MigrationCommandInput};
use crate::migration_engine::MigrationEngine;
use datamodel::dml::Datamodel;
use migration_connector::*;

pub struct CalculateDatamodelCommand {
    input: CalculateDatamodelInput,
}

impl MigrationCommand for CalculateDatamodelCommand {
    type Input = CalculateDatamodelInput;
    type Output = CalculateDatamodelOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(CalculateDatamodelCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        println!("{:?}", self.input);

        let base_datamodel = Datamodel::empty();
        let datamodel = engine.datamodel_calculator().infer(&base_datamodel, &self.input.steps);
        Ok(CalculateDatamodelOutput {
            datamodel: datamodel::render(&datamodel).unwrap(),
        })
    }

    fn must_initialize_engine(&self) -> bool { 
        false
    }
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CalculateDatamodelInput {
    pub steps: Vec<MigrationStep>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CalculateDatamodelOutput {
    pub datamodel: String,
}

impl MigrationCommandInput for CalculateDatamodelInput {
    fn source_config(&self) -> Option<&str> {
        None
    }
}
