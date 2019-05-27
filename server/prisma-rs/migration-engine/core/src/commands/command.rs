use crate::migration_engine::MigrationEngine;
use serde::de::DeserializeOwned;
use serde::Serialize;
use migration_connector::*;

pub trait MigrationCommand {
    type Input: DeserializeOwned;
    type Output: Serialize;

    fn new(input: Self::Input) -> Box<Self>;

    fn execute<T: DatabaseMigrationStepExt>(&self, engine: &MigrationEngine<T>) -> Self::Output;
}
