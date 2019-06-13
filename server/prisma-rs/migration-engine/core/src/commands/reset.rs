use crate::commands::command::*;
use crate::migration_engine::MigrationEngine;

pub struct ResetCommand {
}

#[allow(unused)]
impl MigrationCommand for ResetCommand {
    type Input = serde_json::Value;
    type Output = serde_json::Value;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(ResetCommand {})
    }

    fn execute(&self, engine: &MigrationEngine) -> CommandResult<Self::Output> {
        engine.reset();
        engine.init();
        Ok(serde_json::from_str("{}").unwrap())
    }

    fn has_source_config() -> bool {
        true
    }

    fn underlying_database_must_exist() -> bool {
        true
    }
}
