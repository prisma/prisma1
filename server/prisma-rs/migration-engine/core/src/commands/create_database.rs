use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct CreateDatabaseCommand<'a> {
    input: &'a CreateDatabaseInput,
}

impl<'a> MigrationCommand<'a> for CreateDatabaseCommand<'a> {
    type Input = CreateDatabaseInput;
    type Output = CreateDatabaseOutput;

    fn new(input: &'a Self::Input) -> Box<Self> {
        Box::new(CreateDatabaseCommand { input })
    }

    fn execute<C, D>(&self, engine: &MigrationEngine<C, D>) -> CommandResult<Self::Output>
    where
        C: MigrationConnector<DatabaseMigration = D>,
        D: DatabaseMigrationMarker + 'static,
    {
        debug!("{:?}", self.input);
        let connector = engine.connector();

        let result = CreateDatabaseOutput {};
        Ok(result)
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateDatabaseInput {}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateDatabaseOutput {}
