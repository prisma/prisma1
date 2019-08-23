use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use migration_connector::steps::*;
use migration_connector::*;

pub struct ListMigrationStepsCommand;

impl<'a> MigrationCommand<'a> for ListMigrationStepsCommand {
    type Input = serde_json::Value;
    type Output = Vec<ListMigrationStepsOutput>;

    fn new(_: &'a Self::Input) -> Box<Self> {
        Box::new(ListMigrationStepsCommand {})
    }

    fn execute<C, D>(&self, engine: &MigrationEngine<C, D>) -> CommandResult<Self::Output>
    where
        C: MigrationConnector<DatabaseMigration = D>,
        D: DatabaseMigrationMarker + 'static,
    {
        let migration_persistence = engine.connector().migration_persistence();
        let mut result = Vec::new();

        for migration in migration_persistence.load_all().into_iter() {
            result.push(convert_migration_to_list_migration_steps_output(&engine, migration)?);
        }

        Ok(result)
    }
}

pub fn convert_migration_to_list_migration_steps_output<C, D>(
    engine: &MigrationEngine<C, D>,
    migration: Migration,
) -> CommandResult<ListMigrationStepsOutput>
where
    C: MigrationConnector<DatabaseMigration = D>,
    D: DatabaseMigrationMarker + 'static,
{
    let connector = engine.connector();
    let database_migration = connector.deserialize_database_migration(migration.database_migration);
    let database_steps_json = connector
        .database_migration_step_applier()
        .render_steps_pretty(&database_migration)?;

    Ok(ListMigrationStepsOutput {
        id: migration.name,
        datamodel_steps: migration.datamodel_steps,
        database_steps: database_steps_json,
        status: migration.status,
        datamodel: engine.render_datamodel(&migration.datamodel),
    })
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ListMigrationStepsOutput {
    pub id: String,
    pub datamodel_steps: Vec<MigrationStep>,
    pub database_steps: serde_json::Value,
    pub status: MigrationStatus,
    pub datamodel: String,
}
