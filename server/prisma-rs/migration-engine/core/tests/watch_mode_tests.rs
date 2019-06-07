#![allow(non_snake_case)]
mod test_harness;
use database_inspector::*;
use datamodel::dml::*;
use migration_connector::steps::*;
use migration_core::commands::*;
use migration_core::MigrationEngine;
use test_harness::*;

#[test]
fn watch_mode_must_work() {
    run_test_with_engine(|engine| {
        let migration_persistence = engine.connector().migration_persistence();

        let steps = vec![
            MigrationStep::CreateModel(CreateModel {
                name: "Test".to_string(),
                db_name: None,
                embedded: false,
            }),
            MigrationStep::CreateField(CreateField {
                model: "Test".to_string(),
                name: "id".to_string(),
                tpe: FieldType::Base(ScalarType::Int),
                arity: FieldArity::Required,
                db_name: None,
                is_created_at: None,
                is_updated_at: None,
                is_unique: false,
                id: Some(IdInfo {
                    strategy: IdStrategy::Auto,
                    sequence: None,
                }),
                default: None,
                scalar_list: None,
            }),
        ];

        let db_schema_1 = up(&engine, steps.clone(), "watch");
        let migrations = migration_persistence.load_all();
        assert_eq!(migrations.len(), 1);
        assert_eq!(migrations.first().unwrap().name, "watch");

        let custom_migration_id = "a-custom-migration-id";
        let db_schema_2 = up(&engine, steps.clone(), custom_migration_id);
        assert_eq!(db_schema_1, db_schema_2);
        let migrations = migration_persistence.load_all();
        assert_eq!(migrations.len(), 1);
        assert_eq!(migrations.first().unwrap().name, custom_migration_id);
    });
}

fn up(engine: &Box<MigrationEngine>, steps: Vec<MigrationStep>, migration_id: &str) -> DatabaseSchema {
    let project_info = "the-project-info".to_string();
    let input = ApplyMigrationInput {
        project_info: project_info,
        migration_id: migration_id.to_string(),
        steps: steps,
        force: None,
        dry_run: None,
    };
    let cmd = ApplyMigrationCommand::new(input);
    let _output = cmd.execute(&engine);

    introspect_database(&engine)
}
