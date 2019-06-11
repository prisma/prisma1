use super::MigrationStepsResultOutput;
use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;
use migration_connector::*;
use datamodel::Datamodel;

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
            Some(ref last_migration) if last_migration.is_watch_migration() && !self.input.is_watch_migration() => {
                self.handle_transition_out_of_watch_mode(&engine)
            }
            _ => self.handle_normal_migration(&engine),
        }
    }
}

impl ApplyMigrationCommand {
    fn handle_transition_out_of_watch_mode(
        &self,
        engine: &Box<MigrationEngine>,
    ) -> CommandResult<MigrationStepsResultOutput> {
        let connector = engine.connector();
        let migration_persistence = connector.migration_persistence();

        let current_datamodel = migration_persistence.current_datamodel();
        let last_non_watch_datamodel = migration_persistence.last_non_watch_datamodel();
        let next_datamodel = engine
            .datamodel_calculator()
            .infer(&last_non_watch_datamodel, &self.input.steps);


        self.handle_migration(&engine, current_datamodel, next_datamodel)
    }

    fn handle_normal_migration(&self, engine: &Box<MigrationEngine>) -> CommandResult<MigrationStepsResultOutput> {
        let connector = engine.connector();
        let migration_persistence = connector.migration_persistence();
        let current_datamodel = migration_persistence.current_datamodel();

        let next_datamodel = engine
            .datamodel_calculator()
            .infer(&current_datamodel, &self.input.steps);

        self.handle_migration(&engine, current_datamodel, next_datamodel)
    }

    fn handle_migration(&self, engine: &Box<MigrationEngine>, current_datamodel: Datamodel, next_datamodel: Datamodel) -> CommandResult<MigrationStepsResultOutput> {
        let connector = engine.connector();
        let migration_persistence = connector.migration_persistence();

        let database_migration =
            connector
                .database_migration_inferrer()
                .infer(&current_datamodel, &next_datamodel, &self.input.steps); // TODO: those steps are a lie right now. Does not matter because we don't use them at the moment.

        let database_steps_json_pretty = connector
            .database_migration_step_applier()
            .render_steps_pretty(&database_migration);

        let database_migration_json = database_migration.serialize();
        
        let mut migration = Migration::new(self.input.migration_id.clone());
        migration.datamodel_steps = self.input.steps.clone();
        migration.database_migration = database_migration_json;
        migration.datamodel = next_datamodel.clone();
        let saved_migration = migration_persistence.create(migration);

        connector
            .migration_applier()
            .apply(&saved_migration, &database_migration);
        

        Ok(MigrationStepsResultOutput {
            datamodel: datamodel::render(&next_datamodel).unwrap(),
            datamodel_steps: self.input.steps.clone(),
            database_steps: database_steps_json_pretty,
            errors: Vec::new(),
            warnings: Vec::new(),
            general_errors: Vec::new(),
        })
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ApplyMigrationInput {
    pub migration_id: String,
    pub steps: Vec<MigrationStep>,
    pub force: Option<bool>,
}

impl IsWatchMigration for ApplyMigrationInput {
    fn is_watch_migration(&self) -> bool {
        self.migration_id.starts_with("watch")
    }
}