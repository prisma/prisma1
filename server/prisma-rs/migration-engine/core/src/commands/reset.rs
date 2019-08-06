use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct ResetCommand;

impl<'a> MigrationCommand<'a> for ResetCommand {
    type Input = serde_json::Value;
    type Output = serde_json::Value;

    fn new(_: &'a Self::Input) -> Box<Self> {
        Box::new(Self)
    }

    fn execute<C, D>(&self, engine: &MigrationEngine<C, D>) -> CommandResult<Self::Output>
    where
        C: MigrationConnector<DatabaseMigration = D>,
        D: DatabaseMigrationMarker + 'static,
    {
        engine.reset()?;
        engine.init()?;

        Ok(json!({}))
    }

    fn has_source_config() -> bool {
        true
    }

    fn underlying_database_must_exist() -> bool {
        true
    }
}
