use crate::commands::command::{CommandResult, MigrationCommand, MigrationCommandInput};
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
        let json_string = serde_json::to_string(&self.input.data_sources).unwrap();
        let sources = datamodel::sources_from_json(&json_string);        

        Ok(DmmfToDmlCommandOutput {
            datamodel: datamodel::render_with_sources(&datamodel, &sources).unwrap(),
        })
    }

    fn must_initialize_engine(&self) -> bool { 
        false
    }
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DmmfToDmlCommandInput {
    pub project_info: String,
    pub dmmf: String,
    pub data_sources: serde_json::Value,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DmmfToDmlCommandOutput {
    pub datamodel: String,
}

impl MigrationCommandInput for DmmfToDmlCommandInput {
    fn config(&self) -> &str {
        &self.project_info
    }
}