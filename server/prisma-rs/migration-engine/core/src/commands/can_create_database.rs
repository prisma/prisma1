use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct CanCreateDatabaseCommand<'a> {
    input: &'a CanCreateDatabaseInput,
}

impl<'a> MigrationCommand<'a> for CanCreateDatabaseCommand<'a> {
    type Input = CanCreateDatabaseInput;
    type Output = CanCreateDatabaseOutput;

    fn new(input: &'a Self::Input) -> Box<Self> {
        Box::new(CanCreateDatabaseCommand { input })
    }

    fn execute<C, D>(&self, engine: &MigrationEngine<C, D>) -> CommandResult<Self::Output>
    where
        C: MigrationConnector<DatabaseMigration = D>,
        D: DatabaseMigrationMarker + 'static,
    {
        debug!("{:?}", self.input);
        let connector = engine.connector();

        let result = CanCreateDatabaseOutput {
            result: connector.can_create_database(),
        };
        Ok(result)
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanCreateDatabaseInput {}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CanCreateDatabaseOutput {
    pub result: bool,
}
