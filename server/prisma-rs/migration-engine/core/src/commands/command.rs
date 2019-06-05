use crate::migration_engine::MigrationEngine;
use serde::de::DeserializeOwned;
use serde::Serialize;
use std::convert::{From, Into};

pub trait MigrationCommand {
    type Input: DeserializeOwned;
    type Output: Serialize;

    fn new(input: Self::Input) -> Box<Self>;

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output>;
}

pub type CommandResult<T> = Result<T, CommandError>;

#[derive(Debug)]
pub struct CommandError {
    code: i64,
    message: String
}

impl CommandError {
    pub fn new(code: i64, message: String) -> CommandError {
        CommandError { code, message }
    }
}

impl From<datamodel::errors::ErrorCollection> for CommandError {
    fn from(errors: datamodel::errors::ErrorCollection) -> CommandError {
        unimplemented!()
    }
}