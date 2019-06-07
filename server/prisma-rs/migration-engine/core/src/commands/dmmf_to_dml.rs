use super::DataSource;
use crate::commands::command::{CommandResult, MigrationCommand};
use crate::migration_engine::MigrationEngine;
use datamodel;

pub struct DmmfToDmlCommand {
    input: DmmfToDmlCommandInput,
}

impl MigrationCommand for DmmfToDmlCommand {
    type Input = DmmfToDmlCommandInput;
    type Output = DmmfToDmlCommandOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(DmmfToDmlCommand { input })
    }

    fn execute(&self, _engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        println!("{:?}", self.input);
        let datamodel = datamodel::dmmf::parse_from_dmmf(&self.input.dmmf);
        let boxed_sources = self.input.data_sources.iter().map(|s| s.as_dml_source()).collect();

        Ok(DmmfToDmlCommandOutput {
            datamodel: datamodel::render_with_sources(&datamodel, &boxed_sources).unwrap(),
        })
    }
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DmmfToDmlCommandInput {
    pub project_info: String,
    pub dmmf: String,
    pub data_sources: Vec<DataSource>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DmmfToDmlCommandOutput {
    pub datamodel: String,
}
