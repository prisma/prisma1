pub mod commands;
pub mod connector_loader;
pub mod migration;
pub mod migration_engine;
pub mod rpc_api;

#[macro_use]
extern crate serde_derive;

pub use migration_engine::*;
use commands::{CommandResult, CommandError};
use datamodel::Datamodel;

pub fn parse_datamodel(datamodel: &str) -> CommandResult<Datamodel> {
    let result = datamodel::parse_with_formatted_error(&datamodel, "datamodel file, line");
    result.map_err(|e| CommandError::Generic{code: 1001, error: e})
}
