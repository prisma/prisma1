#![allow(non_snake_case)]
#![allow(unused)]

use barrel::{backend::Sqlite as Squirrel, types, Migration};
use database_inspector::*;
use rusqlite::{Connection, Result, NO_PARAMS};

const SCHEMA: &str = "test_schema";

#[test]
fn columns_of_type_int_must_work() {
    let inspector = setup(|mut migration| {
        migration.create_table("User", |t| {
            t.add_column("id", types::integer());
        });
    });

    let result = inspector.introspect(SCHEMA.to_string());

    let table = result.table("User").unwrap();
    let expected_columns = vec![Column {
        name: "id".to_string(),
        tpe: ColumnType::Int,
        required: false,
        foreign_key: None,
        sequence: None,
    }];

    assert_eq!(table.columns, expected_columns);
}

fn setup<F>(mut migrationFn: F) -> Box<DatabaseInspector>
where
    F: FnMut(&mut Migration) -> (),
{
    let connection = Connection::open_in_memory()
        .and_then(|c| {
            let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
            let path = format!("{}/db", server_root);
            let database_file_path = format!("{}/{}.db", path, SCHEMA);
            std::fs::remove_file(database_file_path.clone()); // ignore potential errors
            c.execute("ATTACH DATABASE ? AS ?", &[database_file_path.as_ref(), SCHEMA])
                .map(|_| c)
        })
        .and_then(|c| {
            c.execute(
                &{
                    let mut migration = Migration::new().schema(SCHEMA);
                    migrationFn(&mut migration);

                    migration.make::<Squirrel>()
                },
                NO_PARAMS,
            )
            .map(|_| c)
        })
        .unwrap();

    Box::new(DatabaseInspectorImpl::new(connection))
}
