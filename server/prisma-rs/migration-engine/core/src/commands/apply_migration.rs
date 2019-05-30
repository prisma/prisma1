use super::MigrationStepsResultOutput;
use crate::commands::command::MigrationCommand;
use crate::migration_engine::MigrationEngine;
use datamodel::dml::Schema;
use migration_connector::*;

pub struct ApplyMigrationCommand {
    input: ApplyMigrationInput,
}

impl MigrationCommand for ApplyMigrationCommand {
    type Input = ApplyMigrationInput;
    type Output = MigrationStepsResultOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ApplyMigrationCommand { input })
    }

    fn execute(&self, engine: &Box<MigrationEngine>) -> Self::Output {
        println!("{:?}", self.input);
        let is_dry_run = self.input.dry_run.unwrap_or(false);

        let connector = engine.connector();
        let current_data_model = connector
            .migration_persistence()
            .last()
            .map(|m| m.datamodel)
            .unwrap_or(Schema::empty());

        let next_data_model = engine
            .datamodel_calculator()
            .infer(&current_data_model, &self.input.steps);

        let database_migration_steps =
            connector
                .database_steps_inferrer()
                .infer(&current_data_model, &next_data_model, &self.input.steps);

        let database_steps_json = connector
            .database_step_applier()
            .render_steps(&database_migration_steps);

        if !is_dry_run {
            let mut migration = Migration::new(self.input.migration_id.clone());
            migration.datamodel_steps = self.input.steps.clone();
            migration.database_steps = database_steps_json.to_string();
            let saved_migration = connector.migration_persistence().create(migration);

            connector
                .migration_applier()
                .apply_steps(&saved_migration, &database_migration_steps);
        }

        MigrationStepsResultOutput {
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
    pub force: Option<bool>,
    pub dry_run: Option<bool>,
}
