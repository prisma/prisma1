use crate::migration_engine::MigrationEngine;
use serde::de::DeserializeOwned;
use serde::Serialize;
use std::convert::From;

pub trait MigrationCommand {
    type Input: DeserializeOwned;
    type Output: Serialize;

    fn new(input: Self::Input) -> Box<Self>;

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output>;

    fn has_source_config() -> bool {
        true
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SourceConfigInput {
    pub source_config: String,
}

pub type CommandResult<T> = Result<T, CommandError>;

#[derive(Debug, Serialize)]
#[serde(tag = "type")]
pub enum CommandError {
    DataModelErrors{code: i64, errors: Vec<String>},
}

impl From<datamodel::errors::ErrorCollection> for CommandError {
    fn from(errors: datamodel::errors::ErrorCollection) -> CommandError {
        let errors_str = errors.errors.into_iter().map(|e|{
            // let mut msg: Vec<u8> = Vec::new();
            // e.pretty_print(&mut msg, "datamodel", "bla").unwrap();
            // std::str::from_utf8(&msg).unwrap().to_string()
            format!("{}", e)
        }).collect();
        CommandError::DataModelErrors{code: 1000, errors: errors_str}
    }
}
