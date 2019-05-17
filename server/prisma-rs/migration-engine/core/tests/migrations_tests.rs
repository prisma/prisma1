#![allow(non_snake_case)]
mod test_harness;
use database_inspector::*;
use migration_core::commands::*;
use migration_core::*;
use test_harness::*;

#[test]
fn adding_a_scalar_field_must_work() {
    run_test_with_engine(|engine| {
        let dm2 = r#"
            model Test {
                id: String @primary
                int: Int
                float: Float
                boolean: Boolean
                string: String
                dateTime: DateTime
            }
        "#;
        let result = migrate_to(&engine, &dm2);
        let table = result.table_bang("Test");
        table.columns.iter().for_each(|c| assert_eq!(c.is_required, true));

        assert_eq!(table.column_bang("int").tpe, ColumnType::Int);
        assert_eq!(table.column_bang("float").tpe, ColumnType::Float);
        assert_eq!(table.column_bang("boolean").tpe, ColumnType::Boolean);
        assert_eq!(table.column_bang("string").tpe, ColumnType::String);
        assert_eq!(table.column_bang("dateTime").tpe, ColumnType::DateTime);
    });
}

fn migrate_to(engine: &Box<MigrationEngine>, datamodel: &str) -> DatabaseSchema {
    let project_info = "the-project-info".to_string();
    let migration_id = "the-migration-id".to_string();

    let input = InferMigrationStepsInput {
        project_info: project_info.clone(),
        migration_id: migration_id.clone(),
        data_model: datamodel.to_string(),
    };
    let cmd = InferMigrationStepsCommand::new(input);
    let output = cmd.execute(&engine);

    let input = ApplyMigrationInput {
        project_info: project_info,
        migration_id: migration_id,
        steps: output.datamodel_steps,
        force: false,
    };
    let cmd = ApplyMigrationCommand::new(input);
    let engine = MigrationEngine::new();
    let output = cmd.execute(&engine);

    let inspector = engine.connector().database_inspector();
    inspector.introspect(&engine.schema_name())
}
