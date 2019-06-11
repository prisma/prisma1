use crate::commands::command::*;
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
        let config = datamodel::config_from_mcf_json_value(self.input.config.clone());

        Ok(DmmfToDmlCommandOutput {
            datamodel: datamodel::render_with_config(&datamodel, &config).unwrap(),
        })
    }

    fn has_source_config() -> bool {
        false
    }
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct DmmfToDmlCommandInput {
    pub dmmf: String,
    pub config: serde_json::Value,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DmmfToDmlCommandOutput {
    pub datamodel: String,
}
