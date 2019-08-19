use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct CanConnectToDatabaseCommand<'a> {
    input: &'a CanConnectToDatabaseInput,
}

impl<'a> MigrationCommand<'a> for CanConnectToDatabaseCommand<'a> {
    type Input = CanConnectToDatabaseInput;
    type Output = CanConnectToDatabaseOutput;

    fn new(input: &'a Self::Input) -> Box<Self> {
        Box::new(CanConnectToDatabaseCommand { input })
    }

    fn execute<C, D>(&self, engine: &MigrationEngine<C, D>) -> CommandResult<Self::Output>
    where
        C: MigrationConnector<DatabaseMigration = D>,
        D: DatabaseMigrationMarker + 'static,
    {
        debug!("{:?}", self.input);
        let connector = engine.connector();

        let result = CanConnectToDatabaseOutput {
            result: connector.can_connect(),
        };
        Ok(result)
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanConnectToDatabaseInput {}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CanConnectToDatabaseOutput {
    pub result: bool,
}
