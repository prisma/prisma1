use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use datamodel::dml::Schema;
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

    fn execute(&self, engine: &Box<MigrationEngine>) -> Self::Output {
        println!("{:?}", self.input);

        let base_datamodel = Schema::empty();
        let datamodel = engine
            .datamodel_calculator()
            .infer(&base_datamodel, self.input.steps.clone());
        // todo: render the datamodel properly
        CalculateDatamodelOutput {
            datamodel: format!("{:?}", datamodel),
        }
    }
}

#[derive(Deserialize, Debug)]
pub struct CalculateDatamodelInput {
    pub project_info: String,
    pub steps: Vec<MigrationStep>,
}

#[derive(Serialize)]
pub struct CalculateDatamodelOutput {
    pub datamodel: String,
}
