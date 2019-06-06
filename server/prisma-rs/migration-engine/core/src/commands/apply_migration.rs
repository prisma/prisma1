use super::MigrationStepsResultOutput;
use crate::commands::command::{CommandResult, MigrationCommand};
use crate::migration_engine::MigrationEngine;
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

    fn execute(&self, engine: &Box<MigrationEngine>) -> CommandResult<Self::Output> {
        println!("{:?}", self.input);
        let connector = engine.connector();
        let migration_persistence = connector.migration_persistence();

        match migration_persistence.last() {
            Some(ref migration) if migration.is_watch_migration() && !self.input.is_watch_migration() => {
                self.handle_watch_migration(&engine, &migration)
            }
            _ => self.handle_normal_migration(&engine),
        }
    }
}

impl ApplyMigrationCommand {
    fn handle_watch_migration(
        &self,
        engine: &Box<MigrationEngine>,
        migration: &Migration,
    ) -> CommandResult<MigrationStepsResultOutput> {
        assert!(migration.is_watch_migration());
        let connector = engine.connector();
        let migration_persistence = connector.migration_persistence();

        let all_watch_migrations = migration_persistence.load_current_watch_migrations();
        let mut all_steps_from_all_watch_migrations = Vec::new();
        for mut migration in all_watch_migrations.into_iter() {
            all_steps_from_all_watch_migrations.append(&mut migration.datamodel_steps);
        }

        if all_steps_from_all_watch_migrations == self.input.steps {
            // let mut update_params = migration.update_params();
            // update_params.new_name = self.input.migration_id.clone();
            // migration_persistence.update(&update_params);

            // let database_migration = connector.deserialize_database_migration(migration.database_migration.clone());
            // let database_steps_json_pretty = connector
            //     .database_migration_step_applier()
            //     .render_steps_pretty(&database_migration);

            let database_migration = connector.empty_database_migration();
            let database_steps_json_pretty = connector
                .database_migration_step_applier()
                .render_steps_pretty(&database_migration);

            let mut migration = Migration::new(self.input.migration_id.clone());
            migration.datamodel_steps = self.input.steps.clone();
            migration.database_migration = database_migration.serialize();
            migration.datamodel = migration_persistence.current_datamodel();
            migration.mark_as_finished();
            let _ = migration_persistence.create(migration);

            Ok(MigrationStepsResultOutput {
                datamodel_steps: self.input.steps.clone(),
                database_steps: database_steps_json_pretty,
                errors: Vec::new(),
                warnings: Vec::new(),
                general_errors: Vec::new(),
            })
        } else {
            Ok(MigrationStepsResultOutput {
                datamodel_steps: self.input.steps.clone(),
                database_steps: serde_json::Value::Array(Vec::new()),
                errors: Vec::new(),
                warnings: Vec::new(),
                general_errors: vec!["The last saved migration is a watch migration. The provided steps don't match the ones of this watch migration. This is disallowed.".to_string()],
            })
        }
    }

    fn handle_normal_migration(&self, engine: &Box<MigrationEngine>) -> CommandResult<MigrationStepsResultOutput> {
        let connector = engine.connector();
        let migration_persistence = connector.migration_persistence();
        let current_datamodel = migration_persistence.current_datamodel();
        let is_dry_run = self.input.dry_run.unwrap_or(false);

        let next_datamodel = engine
            .datamodel_calculator()
            .infer(&current_datamodel, &self.input.steps);

        let database_migration =
            connector
                .database_migration_inferrer()
                .infer(&current_datamodel, &next_datamodel, &self.input.steps);

        let database_steps_json_pretty = connector
            .database_migration_step_applier()
            .render_steps_pretty(&database_migration);

        let database_migration_json = database_migration.serialize();

        if !is_dry_run {
            let mut migration = Migration::new(self.input.migration_id.clone());
            migration.datamodel_steps = self.input.steps.clone();
            migration.database_migration = database_migration_json;
            migration.datamodel = next_datamodel;
            let saved_migration = migration_persistence.create(migration);

            connector
                .migration_applier()
                .apply(&saved_migration, &database_migration);
        }

        Ok(MigrationStepsResultOutput {
            datamodel_steps: self.input.steps.clone(),
            database_steps: database_steps_json_pretty,
            errors: Vec::new(),
            warnings: Vec::new(),
            general_errors: Vec::new(),
        })
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

impl IsWatchMigration for ApplyMigrationInput {
    fn is_watch_migration(&self) -> bool {
        self.migration_id.starts_with("watch")
    }
}
