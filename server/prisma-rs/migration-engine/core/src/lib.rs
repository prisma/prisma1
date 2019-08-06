pub mod api;
pub mod commands;
mod error;
pub mod migration;
pub mod migration_engine;

#[macro_use]
extern crate log;
#[macro_use]
extern crate serde_derive;
#[macro_use]
extern crate serde_json;
#[macro_use]
extern crate neon;

use api::JavascriptApiExport;
use commands::*;
use datamodel::{self, Datamodel};

pub use error::Error;
pub use migration_engine::*;

pub fn parse_datamodel(datamodel: &str) -> CommandResult<Datamodel> {
    let result = datamodel::parse_with_formatted_error(&datamodel, "datamodel file, line");
    result.map_err(|e| CommandError::Generic { code: 1001, error: e })
}

pub type Result<T> = std::result::Result<T, Error>;

register_module!(mut m, { m.export_class::<JavascriptApiExport>("MigrationEngine") });
