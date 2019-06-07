#![allow(non_snake_case)]
mod test_harness;
use database_inspector::*;
use datamodel::dml::*;
use migration_connector::steps::*;
use migration_connector::*;
use migration_core::commands::*;
use migration_core::MigrationEngine;
use test_harness::*;

#[test]
fn single_watch_migrations_must_work() {
    run_test_with_engine(|engine| {
        let migration_persistence = engine.connector().migration_persistence();

        let steps = vec![
            create_model_step("Test"),
            create_id_field_step("Test", "id", ScalarType::Int),
        ];

        let db_schema_1 = up(&engine, steps.clone(), "watch-0001");
        let migrations = migration_persistence.load_all();
        assert_eq!(migrations.len(), 1);
        assert_eq!(migrations.first().unwrap().name, "watch-0001");

        let custom_migration_id = "a-custom-migration-id";
        let db_schema_2 = up(&engine, steps.clone(), custom_migration_id);
        assert_eq!(db_schema_1, db_schema_2);
        let migrations = migration_persistence.load_all();

        assert_eq!(migrations.len(), 2);
        assert_eq!(migrations[0].name, "watch-0001");
        assert_eq!(migrations[1].name, custom_migration_id);
        assert_eq!(migrations[1].status, MigrationStatus::Success);
        assert!(migrations[1].finished_at.is_some());
    });
}

#[test]
fn multiple_watch_migrations_must_work() {
    run_test_with_engine(|engine| {
        let migration_persistence = engine.connector().migration_persistence();

        let steps1 = vec![
            create_model_step("Test"),
            create_id_field_step("Test", "id", ScalarType::Int),
        ];

        let _ = up(&engine, steps1.clone(), "watch-0001");
        let migrations = migration_persistence.load_all();
        assert_eq!(migrations.len(), 1);
        assert_eq!(migrations[0].name, "watch-0001");

        let steps2 = vec![create_field_step("Test", "field", ScalarType::String)];
        let db_schema_2 = up(&engine, steps2.clone(), "watch-0002");
        let migrations = migration_persistence.load_all();
        assert_eq!(migrations.len(), 2);
        assert_eq!(migrations[0].name, "watch-0001");
        assert_eq!(migrations[1].name, "watch-0002");

        let custom_migration_id = "a-custom-migration-id";
        let mut final_steps = Vec::new();
        final_steps.append(&mut steps1.clone());
        final_steps.append(&mut steps2.clone());

        let final_db_schema = up(&engine, final_steps, custom_migration_id);
        assert_eq!(db_schema_2, final_db_schema);
        let migrations = migration_persistence.load_all();
        assert_eq!(migrations.len(), 3);
        assert_eq!(migrations[0].name, "watch-0001");
        assert_eq!(migrations[1].name, "watch-0002");
        assert_eq!(migrations[2].name, custom_migration_id);
        assert_eq!(migrations[2].status, MigrationStatus::Success);
        assert!(migrations[2].finished_at.is_some());
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
    let output = cmd.execute(&engine).expect("ApplyMigration failed");
    assert!(
        output.general_errors.is_empty(),
        format!("ApplyMigration returned unexpected errors: {:?}", output.general_errors)
    );

    introspect_database(&engine)
}
