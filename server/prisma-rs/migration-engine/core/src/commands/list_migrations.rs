use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use migration_connector::steps::*;
use migration_connector::*;

pub struct ListMigrationStepsCommand {
    input: ListMigrationStepsInput,
}

#[allow(unused)]
impl MigrationCommand for ListMigrationStepsCommand {
    type Input = ListMigrationStepsInput;
    type Output = Vec<ListMigrationStepsOutput>;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ListMigrationStepsCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        println!("{:?}", self.input);
        let connector = engine.connector();
        let migration_persistence = engine.connector().migration_persistence();
        Ok(migration_persistence
            .load_all()
            .into_iter()
            .map(|mig| convert_migration_to_list_migration_steps_output(&engine, mig))
            .collect())
    }
}

pub fn convert_migration_to_list_migration_steps_output(
    engine: &Box<MigrationEngine>,
    migration: Migration,
) -> ListMigrationStepsOutput {
    let connector = engine.connector();
    let database_migration = connector.deserialize_database_migration(migration.database_migration);
    let database_steps_json = connector
        .database_migration_step_applier()
        .render_steps_pretty(&database_migration);

    ListMigrationStepsOutput {
        id: migration.name,
        datamodel_steps: migration.datamodel_steps,
        database_steps: database_steps_json,
        status: migration.status,
        datamodel: engine.render_datamodel(&migration.datamodel),
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ListMigrationStepsInput {
    pub source_config: String,
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