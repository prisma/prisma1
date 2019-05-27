//use migration_core::commands::command::*;
//use migration_core::commands::infer_migration_steps::*;
//use migration_core::migration_engine::MigrationEngine;
//use std::fs;

fn main() {
    unimplemented!("Disabled for now, need to parse and inject connection details.")
    /*
    let data_model = fs::read_to_string("datamodel.prisma").unwrap();
    let input = InferMigrationStepsInput {
        project_info: "the-project-info".to_string(),
        migration_id: "the-migration-id".to_string(),
        data_model: data_model,
    };
    let cmd = InferMigrationStepsCommand::new(input);
    let engine = MigrationEngine::new();
    let output = cmd.execute(&engine);

    let json = serde_json::to_string_pretty(&output).unwrap();
    println!("{}", json)
    */
}
