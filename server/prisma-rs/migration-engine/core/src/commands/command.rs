use crate::migration_engine::MigrationEngine;
use migration_connector::*;
use failure::Fail;
use serde::de::DeserializeOwned;
use serde::Serialize;
use std::convert::From;

pub trait MigrationCommand<'a> {
    type Input: DeserializeOwned + 'a;
    type Output: Serialize;

    fn new(input: &'a Self::Input) -> Box<Self>;

    fn execute<C, D>(&self, engine: &MigrationEngine<C, D>) -> CommandResult<Self::Output>
    where
        C: MigrationConnector<DatabaseMigration = D>,
        D: DatabaseMigrationMarker + 'static;
}

pub type CommandResult<T> = Result<T, CommandError>;

#[derive(Debug, Serialize, Fail)]
#[serde(tag = "type")]
pub enum CommandError {
    #[fail(display = "Errors in datamodel. (code: {}, errors: {:?})", code, errors)]
    DataModelErrors { code: i64, errors: Vec<String> },

    #[fail(display = "Initialization error. (code: {}, error: {})", code, error)]
    InitializationError { code: i64, error: String },

    #[fail(display = "Generic error. (code: {}, error: {})", code, error)]
    Generic { code: i64, error: String },

    #[fail(display = "Error in command input. (code: {}, error: {})", code, error)]
    Input { code: i64, error: String }
}

impl From<datamodel::errors::ErrorCollection> for CommandError {
    fn from(errors: datamodel::errors::ErrorCollection) -> CommandError {
        let errors_str = errors
            .errors
            .into_iter()
            .map(|e| {
                // let mut msg: Vec<u8> = Vec::new();
                // e.pretty_print(&mut msg, "datamodel", "bla").unwrap();
                // std::str::from_utf8(&msg).unwrap().to_string()
                format!("{}", e)
            })
            .collect();
        CommandError::DataModelErrors {
            code: 1001,
            errors: errors_str,
        }
    }
}

impl From<migration_connector::ConnectorError> for CommandError {
    fn from(error: migration_connector::ConnectorError) -> CommandError {
        CommandError::Generic {
            code: 1000,
            error: format!("{:?}", error),
        }
    }
}
