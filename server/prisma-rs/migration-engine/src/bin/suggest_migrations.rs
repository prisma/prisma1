use migration_engine::commands::suggest_migration_step::*;
use migration_engine::commands::command::*;
use std::fs;

fn main() {
    let data_model = fs::read_to_string("datamodel.prisma").unwrap();
    let input = SuggestMigrationStepsInput {
        project: "the-project-id".to_string(),
        data_model: data_model,
    };
    let cmd = SuggestMigrationStepsCommand::new(input);
    let output = cmd.execute();

    let json = serde_json::to_string_pretty(&output).unwrap();
    println!("{}", json)
}