use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;

pub struct ListDataSourcesCommand {
    input: ListDataSourcesInput,
}

#[allow(unused)]
impl MigrationCommand for ListDataSourcesCommand {
    type Input = ListDataSourcesInput;
    type Output = serde_json::Value;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ListDataSourcesCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        println!("{:?}", self.input);
        let sources = datamodel::load_data_source_configuration(&self.input.datamodel)?;
        let json = datamodel::render_sources_to_json_value(&sources);
        Ok(json)
    }

    fn has_source_config() -> bool {
        false
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ListDataSourcesInput {
    pub datamodel: String,
}