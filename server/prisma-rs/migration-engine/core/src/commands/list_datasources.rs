use crate::commands::command::{CommandResult, MigrationCommand, MigrationCommandInput};
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

    fn must_initialize_engine(&self) -> bool { 
        false
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ListDataSourcesInput {
    pub datamodel: String,
}

impl MigrationCommandInput for ListDataSourcesInput {
    fn source_config(&self) -> Option<&str> {
        None
    }    
}