use migration_connector::steps::*;
use migration_core::commands::*;
use migration_core::migration_engine::MigrationEngine;
use std::io::{self, Read};

fn main() {
    let mut buffer = String::new();
    io::stdin().read_to_string(&mut buffer).unwrap();

    let steps: Vec<MigrationStep> = serde_json::from_str(&buffer).expect("deserializing the migration steps failed");

    let input = ApplyMigrationInput {
        project_info: "the-project-info".to_string(),
        migration_id: "the-migration-id".to_string(),
        steps: steps,
        force: None,
        dry_run: None,
    };
    let cmd = ApplyMigrationCommand::new(input);
    let engine = MigrationEngine::new();
    let output = cmd.execute(&engine);

    let json = serde_json::to_string_pretty(&output).unwrap();
    println!("{}", json)
}
