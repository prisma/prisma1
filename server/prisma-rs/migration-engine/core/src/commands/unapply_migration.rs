use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct UnapplyMigrationCommand<'a> {
    input: &'a UnapplyMigrationInput,
}

impl<'a> MigrationCommand<'a> for UnapplyMigrationCommand<'a> {
    type Input = UnapplyMigrationInput;
    type Output = UnapplyMigrationOutput;

    fn new(input: &'a Self::Input) -> Box<Self> {
        Box::new(UnapplyMigrationCommand { input })
    }

    fn execute<C, D>(&self, engine: &MigrationEngine<C, D>) -> CommandResult<Self::Output>
    where
        C: MigrationConnector<DatabaseMigration = D>,
        D: DatabaseMigrationMarker + 'static,
    {
        debug!("{:?}", self.input);
        let connector = engine.connector();

        let result = match connector.migration_persistence().last() {
            None => UnapplyMigrationOutput {
                rolled_back: "not-applicable".to_string(),
                active: None,
                errors: vec!["There is no last migration that can be rolled back.".to_string()],
            },
            Some(migration_to_rollback) => {
                let database_migration =
                    connector.deserialize_database_migration(migration_to_rollback.database_migration.clone());

                connector
                    .migration_applier()
                    .unapply(&migration_to_rollback, &database_migration)?;

                let new_active_migration = connector.migration_persistence().last().map(|m| m.name);

                UnapplyMigrationOutput {
                    rolled_back: migration_to_rollback.name,
                    active: new_active_migration,
                    errors: Vec::new(),
                }
            }
        };

        Ok(result)
    }

    fn underlying_database_must_exist() -> bool {
        true
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UnapplyMigrationInput {}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnapplyMigrationOutput {
    pub rolled_back: String,
    pub active: Option<String>,
    pub errors: Vec<String>,
}
