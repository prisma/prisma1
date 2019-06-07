use super::DataSource;
use crate::commands::command::{CommandResult, MigrationCommand};
use crate::migration_engine::MigrationEngine;

pub struct ListDataSourcesCommand {
    input: ListDataSourcesInput,
}

#[allow(unused)]
impl MigrationCommand for ListDataSourcesCommand {
    type Input = ListDataSourcesInput;
    type Output = Vec<DataSource>;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ListDataSourcesCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        println!("{:?}", self.input);
        let sources = datamodel::load_data_source_configuration(&self.input.datamodel)?;
        let output = sources
            .iter()
            .map(|source| DataSource {
                name: source.name().to_string(),
                tpe: source.connector_type().to_string(),
                url: source.url().to_string(),
            })
            .collect();
        Ok(output)
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ListDataSourcesInput {
    pub project_info: String,
    pub datamodel: String,
}
