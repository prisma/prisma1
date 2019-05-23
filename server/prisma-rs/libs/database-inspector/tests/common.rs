use barrel;
use database_inspector::*;
use prisma_query::{transaction::Connectional, connector::Sqlite};

pub const SCHEMA: &'static str = "RELATIONAL_INTROSPECTION_TEST_SCHEMA";


/// Removes the database file from disk.
/// 
/// This should, at one point, move into prisma-query, since it depends
/// on the connector internal logic of resolving DBs.
pub fn remove_db(path: &str) {
    let database_file_path = format!("{}/{}.db", path, SCHEMA);
    std::fs::remove_file(&database_file_path); // ignore potential errors
    std::thread::sleep(std::time::Duration::from_millis(100));
}

pub fn run_test<F, T>(mut migration_fn: F, mut test_fn: T) -> Result<(), SqlError>
where
    F: FnMut(&mut barrel::Migration) -> (),
    T: FnMut(&mut Connection) -> Result<(), SqlError>,
{
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let path = format!("{}/db", server_root);

    remove_db(&path);

    let client = Sqlite::new(path, 32, false)?;

    let mut migration = barrel::Migration::new().schema(SCHEMA);

    // Generates the setup migration
    migration_fn(&mut migration);

    // TODO: Make backend variadic.
    let full_sql = migration.make::<barrel::backend::Sqlite>();

    client.with_connection(SCHEMA, |connection| {
        for sql in full_sql.split(";") {
            if sql != "" {
                connection.query_raw(&sql, &[]).unwrap();
            }
        }
        Ok(())
    })?;

    // Executes introspection and tests the result

    client.with_connection(SCHEMA, |connection| {
        test_fn(connection)
    })
}