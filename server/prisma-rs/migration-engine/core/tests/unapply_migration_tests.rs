#![allow(non_snake_case)]
mod test_harness;
use database_inspector::*;
use migration_core::commands::*;
use migration_core::MigrationEngine;
use test_harness::*;

#[test]
fn unapply_must_work() {
    run_test_with_engine(|engine| {
        let dm1 = r#"
            model Test {
                id: String @id
                field: String
            }
        "#;
        let result1 = infer_and_apply(&engine, &dm1);
        assert_eq!(result1.table_bang("Test").column("field").is_some(), true);

        let dm2 = r#"
            model Test {
                id: String @id
            }
        "#;
        let result2 = infer_and_apply(&engine, &dm2);
        assert_eq!(result2.table_bang("Test").column("field").is_some(), false);

        let result3 = unapply_last_migration(&engine);
        assert_eq!(result1, result3);

        // reapply the migration again
        let result4 = infer_and_apply(&engine, &dm2);
        assert_eq!(result2, result4);
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
