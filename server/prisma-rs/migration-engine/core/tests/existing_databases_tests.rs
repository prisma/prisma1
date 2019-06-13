#![allow(non_snake_case)]
mod test_harness;
use barrel::{ types, Migration, SqlVariant};
use database_inspector::*;
use test_harness::*;
use std::sync::Arc;
use prisma_query::Connectional;
use migration_core::MigrationEngine;
use sql_migration_connector::SqlFamily;

#[test]
fn adding_a_model_for_an_existing_table_must_work() {
    test_each_backend(|engine,barrel| {
        let initial_result = barrel.execute(|migration| {
            migration.create_table("Blog", |t| {
                t.add_column("id", types::primary());
            });
        });
        let dm = r#"
            model Blog {
                id Int @id
            }
        "#;
        let result = infer_and_apply(&engine, &dm);
        assert_eq!(initial_result, result);
    });
}

#[test]
fn bigint_columns_must_work() {
    // TODO: port when barrel supports arbitray primary keys
}

#[test]
fn removing_a_model_for_a_table_that_is_already_deleted_must_work() {
    test_each_backend(|engine,barrel| {
        let dm1 = r#"
            model Blog {
                id Int @id
            }

            model Post {
                id Int @id
            }
        "#;
        let initial_result = infer_and_apply(&engine, &dm1);
        assert_eq!(initial_result.table("Post").is_some(), true);

        let result = barrel.execute(|migration| {
            migration.drop_table("Post");
        });
        assert_eq!(result.table("Post").is_some(), false);

        let dm2 = r#"
            model Blog {
                id Int @id
            }
        "#;
        let final_result = infer_and_apply(&engine, &dm2);
        assert_eq!(result, final_result);
    });
}

#[test]
fn creating_a_field_for_an_existing_column_with_a_compatible_type_must_work() {
    test_each_backend(|engine,barrel| {
        let initial_result = barrel.execute(|migration| {
            migration.create_table("Blog", |t| {
                t.add_column("id", types::primary());
                t.add_column("title", types::text());
            });
        });
        let dm = r#"
            model Blog {
                id Int @id
                title String
            }
        "#;
        let result = infer_and_apply(&engine, &dm);
        assert_eq!(initial_result, result);
    });
}

#[test]
fn creating_a_field_for_an_existing_column_and_changing_its_type_must_work() {
    test_each_backend(|engine,barrel| {
        let initial_result = barrel.execute(|migration| {
            migration.create_table("Blog", |t| {
                t.add_column("id", types::primary());
                t.add_column("title", types::integer().nullable(true));
            });
        });
        let initial_column = initial_result.table_bang("Blog").column_bang("title");
        assert_eq!(initial_column.tpe, ColumnType::Int);
        assert_eq!(initial_column.is_required, false);

        let dm = r#"
            model Blog {
                id Int @id
                title String @unique
            }
        "#;
        let result = infer_and_apply(&engine, &dm);
        let column = result.table_bang("Blog").column_bang("title");
        assert_eq!(column.tpe, ColumnType::String);
        assert_eq!(column.is_required, true);
        // TODO: assert uniqueness
    });
}

#[test]
fn creating_a_field_for_an_existing_column_and_simultaneously_making_it_optional() {
    test_each_backend(|engine,barrel| {
        let initial_result = barrel.execute(|migration| {
            migration.create_table("Blog", |t| {
                t.add_column("id", types::primary());
                t.add_column("title", types::text());
            });
        });
        let initial_column = initial_result.table_bang("Blog").column_bang("title");
        assert_eq!(initial_column.is_required, true);

        let dm = r#"
            model Blog {
                id Int @id
                title String?
            }
        "#;
        let result = infer_and_apply(&engine, &dm);
        let column = result.table_bang("Blog").column_bang("title");
        assert_eq!(column.is_required, false);
    });
}

#[test]
fn creating_a_scalar_list_field_for_an_existing_table_must_work() {
    test_each_backend_with_ignores(vec![SqlFamily::Postgres],|engine,barrel| {
        let dm1 = r#"
            model Blog {
                id Int @id
            }
        "#;
        let initial_result = infer_and_apply(&engine, &dm1);
        assert_eq!(initial_result.table("Blog_tags").is_some(), false);

        let result = barrel.execute(|migration| {
            migration.create_table("Blog_tags", |t| {
                t.add_column("nodeId", types::foreign("Blog", "id")); // TODO: barrel does not render this one correctly
                t.add_column("position", types::integer());
                t.add_column("value", types::text());
            });
        });
        assert_eq!(result.table("Blog_tags").is_some(), true);

        let dm2 = r#"
            model Blog {
                id Int @id
                tags String[]
            }
        "#;
        let final_result = infer_and_apply(&engine, &dm2);
        assert_eq!(result, final_result);
    });
}

#[test]
fn delete_a_field_for_a_non_existent_column_must_work() {
    test_each_backend(|engine, barrel| {
        let dm1 = r#"
            model Blog {
                id Int @id
                title String
            }
        "#;
        let initial_result = infer_and_apply(&engine, &dm1);
        assert_eq!(initial_result.table_bang("Blog").column("title").is_some(), true);

        let result = barrel.execute(|migration| {
            // sqlite does not support dropping columns. So we are emulating it..
            migration.drop_table("Blog");
            migration.create_table("Blog", |t| {
                t.add_column("id", types::primary());
            });
        });
        assert_eq!(result.table_bang("Blog").column("title").is_some(), false);

        let dm2 = r#"
            model Blog {
                id Int @id
            }
        "#;
        let final_result = infer_and_apply(&engine, &dm2);
        assert_eq!(result, final_result);
    });
}

#[test]
fn deleting_a_scalar_list_field_for_a_non_existent_list_table_must_work() {
    test_each_backend(|engine, barrel| {
        let dm1 = r#"
            model Blog {
                id Int @id
                tags String[]
            }
        "#;
        let initial_result = infer_and_apply(&engine, &dm1);
        assert_eq!(initial_result.table("Blog_tags").is_some(), true);

        let result = barrel.execute(|migration| {
            migration.drop_table("Blog_tags");
        });
        assert_eq!(result.table("Blog_tags").is_some(), false);

        let dm2 = r#"
            model Blog {
                id Int @id
            }
        "#;
        let final_result = infer_and_apply(&engine, &dm2);
        assert_eq!(result, final_result);
    });
}

#[test]
fn updating_a_field_for_a_non_existent_column() {
    test_each_backend(|engine, barrel| {
        let dm1 = r#"
            model Blog {
                id Int @id
                title String
            }
        "#;
        let initial_result = infer_and_apply(&engine, &dm1);
        let initial_column = initial_result.table_bang("Blog").column_bang("title");
        assert_eq!(initial_column.tpe, ColumnType::String);

        let result = barrel.execute(|migration| {
            // sqlite does not support dropping columns. So we are emulating it..
            migration.drop_table("Blog");
            migration.create_table("Blog", |t| {
                t.add_column("id", types::primary());
            });
        });
        assert_eq!(result.table_bang("Blog").column("title").is_some(), false);

        let dm2 = r#"
            model Blog {
                id Int @id
                title Int @unique
            }
        "#;
        let final_result = infer_and_apply(&engine, &dm2);
        let final_column = final_result.table_bang("Blog").column_bang("title");
        assert_eq!(final_column.tpe, ColumnType::Int);
        // TODO: assert uniqueness
    });
}

#[test]
fn renaming_a_field_where_the_column_was_already_renamed_must_work() {
    test_each_backend(|engine, barrel|{
        let dm1 = r#"
            model Blog {
                id Int @id
                title String
            }
        "#;
        let initial_result = infer_and_apply(&engine, &dm1);
        let initial_column = initial_result.table_bang("Blog").column_bang("title");
        assert_eq!(initial_column.tpe, ColumnType::String);

        let result = barrel.execute(|migration| {
            // sqlite does not support renaming columns. So we are emulating it..
            migration.drop_table("Blog");
            migration.create_table("Blog", |t| {
                t.add_column("id", types::primary());
                t.add_column("new_title", types::text());
            });
        });
        assert_eq!(result.table_bang("Blog").column("new_title").is_some(), true);

        let dm2 = r#"
            model Blog {
                id Int @id
                title Float @db(name: "new_title")
            }
        "#;
        let final_result = infer_and_apply(&engine, &dm2);
        let final_column = final_result.table_bang("Blog").column_bang("new_title");
        assert_eq!(final_column.tpe, ColumnType::Float);
        assert_eq!(final_result.table_bang("Blog").column("title").is_some(), false);
        // TODO: assert uniqueness
    })
}

fn test_each_backend<TestFn>(testFn: TestFn)
    where 
        TestFn: Fn(&MigrationEngine, &BarrelMigrationExecutor) -> () + std::panic::RefUnwindSafe,
{
    test_each_backend_with_ignores(Vec::new(), testFn);
}

fn test_each_backend_with_ignores<TestFn>(ignores: Vec<SqlFamily>, testFn: TestFn)
    where 
        TestFn: Fn(&MigrationEngine, &BarrelMigrationExecutor) -> () + std::panic::RefUnwindSafe,
{
    // SQLite
    if !ignores.contains(&SqlFamily::Sqlite){
        println!("Testing with SQLite now");
        let (inspector, connectional) = sqlite();
        println!("Running the test function now");
        let engine = test_engine(&sqlite_test_config());
        let barrel_migration_executor = BarrelMigrationExecutor {
            inspector,
            connectional,
            sql_variant: SqlVariant::Sqlite,
        };
        testFn(&engine, &barrel_migration_executor);
    } else {
        println!("Ignoring SQLite")
    }
    // POSTGRES
    if !ignores.contains(&SqlFamily::Postgres){
        println!("Testing with Postgres now");
        let (inspector, connectional) = postgres();
        println!("Running the test function now");
        let engine = test_engine(&postgres_test_config());
        let barrel_migration_executor = BarrelMigrationExecutor {
            inspector,
            connectional,
            sql_variant: SqlVariant::Pg,
        };
        testFn(&engine, &barrel_migration_executor);
    } else {
        println!("Ignoring Postgres")
    }
    
}

fn sqlite() -> (Arc<DatabaseInspector>, Arc<Connectional>) {
    let database_file_path = sqlite_test_file();
    let _ = std::fs::remove_file(database_file_path.clone()); // ignore potential errors

    let inspector = DatabaseInspector::sqlite(database_file_path);
    let connectional = Arc::clone(&inspector.connectional);

    (Arc::new(inspector), connectional)
}

fn postgres() -> (Arc<DatabaseInspector>, Arc<Connectional>) {
    let url = postgres_url();
    let drop_schema = dbg!(format!("DROP SCHEMA IF EXISTS \"{}\" CASCADE;", SCHEMA_NAME));
    let setup_connectional = DatabaseInspector::postgres(url.to_string()).connectional;
    let _ = setup_connectional.query_on_raw_connection(&SCHEMA_NAME, &drop_schema, &[]);


    let inspector = DatabaseInspector::postgres(url.to_string());
    let connectional = Arc::clone(&inspector.connectional);

    (Arc::new(inspector), connectional)
}

struct BarrelMigrationExecutor {
    inspector: Arc<DatabaseInspector>,
    connectional: Arc<Connectional>,
    sql_variant: barrel::backend::SqlVariant,
}

impl BarrelMigrationExecutor {
    fn execute<F>(&self, mut migrationFn: F) -> DatabaseSchema
    where
        F: FnMut(&mut Migration) -> (),
    {
        let mut migration = Migration::new().schema(SCHEMA_NAME);
        migrationFn(&mut migration);
        let full_sql = dbg!(migration.make_from(self.sql_variant));
        run_full_sql(&self.connectional, &full_sql);
        let mut result = self.inspector.introspect(&SCHEMA_NAME.to_string());
        // the presence of the _Migration table makes assertions harder. Therefore remove it.
        result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
        result
    }
}

fn run_full_sql(connectional: &Arc<Connectional>, full_sql: &str) {
    for sql in full_sql.split(";") {
        if sql != "" {
            connectional.query_on_raw_connection(&SCHEMA_NAME, &sql, &[]).unwrap();
        }
    }
}

