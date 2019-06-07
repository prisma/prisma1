use crate::commands::command::{CommandResult, MigrationCommand};
use crate::migration_engine::MigrationEngine;

pub struct ListDataSourcesCommand {
    input: ListDataSourcesInput,
}

#[allow(unused)]
impl MigrationCommand for ListDataSourcesCommand {
    type Input = ListDataSourcesInput;
    type Output = Vec<ListDataSourcesOutput>;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ListDataSourcesCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        println!("{:?}", self.input);
        let sources = datamodel::load_data_source_configuration(&self.input.datamodel)?;
        let output = sources.iter().map(|source|{
            ListDataSourcesOutput {
                name: source.name().to_string(),
                tpe: source.connector_type().to_string(),
                url: source.url().to_string(),
            }
        }).collect();
        Ok(output)
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ListDataSourcesInput {
    pub project_info: String,
    pub datamodel: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ListDataSourcesOutput {
    name: String,
    tpe: String, #[serde(rename(serialize = "type"))]
    url: String,
}
