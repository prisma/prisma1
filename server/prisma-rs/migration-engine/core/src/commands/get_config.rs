use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;

pub struct GetConfigCommand {
    input: GetConfigInput,
}

#[allow(unused)]
impl MigrationCommand for GetConfigCommand {
    type Input = GetConfigInput;
    type Output = serde_json::Value;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(GetConfigCommand { input })
    }

    fn execute(&self, engine: &MigrationEngine) -> CommandResult<Self::Output> {
        println!("{:?}", self.input);
        let config = datamodel::load_configuration(&self.input.datamodel)?;
        let json = datamodel::config_to_mcf_json_value(&config);
        Ok(json)
    }

    fn has_source_config() -> bool {
        false
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetConfigInput {
    pub datamodel: String,
}
