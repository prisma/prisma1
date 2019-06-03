#![allow(non_snake_case)]
mod test_harness;
use database_inspector::*;
use test_harness::*;
use migration_core::commands::*;
use migration_core::MigrationEngine;

#[test]
fn unapply_must_work() {
    run_test_with_engine(|engine| {
        let dm = r#"
            model Test {
                id: String @id
                field: String
            }
        "#;
        let result1 = migrate_to(&engine, &dm);
        assert_eq!(result1.table_bang("Test").column("field").is_some(), true);

        let dm = r#"
            model Test {
                id: String @id
            }
        "#;
        let result2 = migrate_to(&engine, &dm);
        assert_eq!(result2.table_bang("Test").column("field").is_some(), false);

        let result3 = unapply_last_migration(&engine);

        assert_eq!(result1, result3);
    });
}

pub fn unapply_last_migration(engine: &Box<MigrationEngine>) -> DatabaseSchema {
    let project_info = "the-project-info".to_string();

    let input = UnapplyMigrationInput {
        project_info: project_info.clone(),
    };
    let cmd = UnapplyMigrationCommand::new(input);
    let _ = cmd.execute(&engine);

    introspect_database(&engine)
}