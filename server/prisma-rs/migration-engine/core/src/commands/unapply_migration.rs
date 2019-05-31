use super::list_migrations::ListMigrationStepsOutput;
use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;

pub struct UnapplyMigrationCommand {
    input: UnapplyMigrationInput,
}
#[allow(unused)]
impl MigrationCommand for UnapplyMigrationCommand {
    type Input = UnapplyMigrationInput;
    type Output = UnapplyMigrationOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(UnapplyMigrationCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> Self::Output {
        println!("{:?}", self.input);
        let connector = engine.connector();
        match connector.migration_persistence().last() {
            None => UnapplyMigrationOutput {
                rolled_back: ListMigrationStepsOutput {
                    id: "foo".to_string(),
                    steps: Vec::new(),
                    status: MigrationStatus::Pending,
                },
                active: ListMigrationStepsOutput {
                    id: "bar".to_string(),
                    steps: Vec::new(),
                    status: MigrationStatus::Pending,
                },
                errors: vec!["There is no last migration that can be rolled back.".to_string()],
            },
            Some(migration_to_rollback) => {
                connector
                    .migration_applier()
                    .unapply_steps(&migration_to_rollback, &Vec::new()); // use actual database steps. but those are still JSON ..

                let new_active_migration = match connector.migration_persistence().last() {
                    Some(m) => m,
                    None => Migration::new("no-migration".to_string()),
                };

                UnapplyMigrationOutput {
                    rolled_back: ListMigrationStepsOutput {
                        id: migration_to_rollback.name,
                        steps: migration_to_rollback.datamodel_steps,
                        status: migration_to_rollback.status,
                    },
                    active: ListMigrationStepsOutput {
                        id: new_active_migration.name,
                        steps: new_active_migration.datamodel_steps,
                        status: new_active_migration.status,
                    },
                    errors: Vec::new(),
                }
            }
        }
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UnapplyMigrationInput {
    pub project_info: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnapplyMigrationOutput {
    pub rolled_back: ListMigrationStepsOutput,
    pub active: ListMigrationStepsOutput,
    pub errors: Vec<String>,
}
