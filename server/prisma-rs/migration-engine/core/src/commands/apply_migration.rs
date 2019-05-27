use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use datamodel::dml::Schema;
use migration_connector::*;

pub struct ApplyMigrationCommand {
    input: ApplyMigrationInput,
}

impl MigrationCommand for ApplyMigrationCommand {
    type Input = ApplyMigrationInput;
    type Output = ApplyMigrationOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ApplyMigrationCommand { input })
    }

    // TODO: Use Result Type and Remove unwrap calls.
    fn execute<T: DatabaseMigrationStepExt>(&self, engine: &MigrationEngine<T>) -> Self::Output {
        println!("{:?}", self.input);
        let connector = engine.connector();
        let current_data_model = connector
            .migration_persistence()
            .last()
            .map(|m| m.datamodel)
            .unwrap_or(Schema::empty());

        let next_data_model = engine
            .datamodel_calculator()
            .infer(&current_data_model, self.input.steps.clone());

        let database_migration_steps =
            connector
                .database_steps_inferrer()
                .infer(&current_data_model, &next_data_model, self.input.steps.clone()).unwrap();

        let database_steps_json = serde_json::to_value(&database_migration_steps).unwrap();

        let mut migration = Migration::new(self.input.migration_id.clone());
        migration.datamodel_steps = self.input.steps.clone();
        migration.database_steps = database_steps_json.to_string();
        let saved_migration = connector.migration_persistence().create(migration).unwrap();

        connector
            .migration_applier()
            .apply_steps(saved_migration, database_migration_steps, connector);

        ApplyMigrationOutput {
            datamodel_steps: self.input.steps.clone(),
            database_steps: database_steps_json,
            errors: Vec::new(),
            warnings: Vec::new(),
            general_errors: Vec::new(),
        }
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApplyMigrationInput {
    pub project_info: String,
    pub migration_id: String,
    pub steps: Vec<MigrationStep>,
    pub force: bool,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ApplyMigrationOutput {
    pub datamodel_steps: Vec<MigrationStep>,
    pub database_steps: serde_json::Value,
    pub warnings: Vec<MigrationWarning>,
    pub errors: Vec<MigrationError>,
    pub general_errors: Vec<String>,
}
