#![allow(non_snake_case)]
mod test_harness;
use database_inspector::relational::{sqlite::*, *};
use database_inspector::*;
use migration_core::commands::*;
use migration_core::*;
use prisma_query::transaction::Connection;
use sql_migration_connector::SqlMigrationStep;
use std::ops::DerefMut;
use test_harness::*;

#[test]
fn adding_a_scalar_field_must_work() {
    run_test_with_engine(|engine, connection| {
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
        let result = migrate_to(connection, &engine, &dm2);
        let table = result.table("Test").unwrap();
        table.columns.iter().for_each(|c| assert_eq!(c.is_nullable, false));

        // TODO: Add common test trait for column_bang
        assert_eq!(table.column("int").unwrap().column_type, ColumnType::Int);
        assert_eq!(table.column("float").unwrap().column_type, ColumnType::Float);
        assert_eq!(table.column("boolean").unwrap().column_type, ColumnType::Boolean);
        assert_eq!(table.column("string").unwrap().column_type, ColumnType::String);
        assert_eq!(table.column("dateTime").unwrap().column_type, ColumnType::DateTime);
    });
}

fn migrate_to(
    connection: &std::cell::RefCell<Connection>,
    engine: &Box<MigrationEngine<SqlMigrationStep>>,
    datamodel: &str,
) -> SchemaInfo {
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
    let output = cmd.execute(&engine);

    let inspector = RelationalIntrospectionConnector::new(Box::new(SqlLiteConnector::new()));
    inspector
        .introspect(connection.borrow_mut().deref_mut(), &engine.schema_name())
        .unwrap()
        .schema
}
