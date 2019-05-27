use barrel;
use datamodel;
#[allow(dead_code)]
use migration_core::MigrationEngine;
use prisma_query::{
    connector::*,
    transaction::{Connection, Connectional},
};
use sql_migration_connector::{SqlMigrationConnector, SqlMigrationStep};
use std::panic;

#[allow(unused)]
pub fn parse(datamodel_string: &str) -> datamodel::Datamodel {
    match datamodel::parse(datamodel_string) {
        Ok(s) => s,
        Err(errs) => {
            for err in errs.to_iter() {
                err.pretty_print(&mut std::io::stderr().lock(), "", datamodel_string)
                    .unwrap();
            }
            panic!("Schema parsing failed. Please see error above.")
        }
    }
}

#[allow(unused)]
const SCHEMA: &str = "migration_tests";

#[allow(unused)]
pub fn run_test_with_engine<T>(test: T) -> ()
where
    T: FnOnce(Box<MigrationEngine<SqlMigrationStep>>, &std::cell::RefCell<Connection>) -> () + panic::UnwindSafe,
{
    // CONNECTION SETUP
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let path = format!("{}/db", server_root);
    remove_db(&path, SCHEMA);

    let client = Sqlite::new(path, 32, false).unwrap();

    // SETUP DATABASE
    let migration = barrel::Migration::new().schema(SCHEMA);
    let full_sql = migration.make::<barrel::backend::Sqlite>();

    client
        .with_connection(SCHEMA, |connection| {
            for sql in full_sql.split(";") {
                if sql != "" {
                    connection.query_raw(&sql, &[]).unwrap();
                }
            }
            Ok(())
        })
        .unwrap();

    // Actual test.
    client
        .with_shared_connection(SCHEMA, |connection| {
            // SETUP

            let connector = SqlMigrationConnector::new(SCHEMA, connection);
            let engine = MigrationEngine::<SqlMigrationStep>::new(&connector, SCHEMA);
            let connector = engine.connector();
            connector.initialize().unwrap();

            // TEST
            test(Box::new(engine), connection);

            Ok(())
        })
        .unwrap();
}

/// Removes the database file from disk.
///
/// This should, at one point, move into prisma-query, since it depends
/// on the connector internal logic of resolving DBs.
pub fn remove_db(path: &str, schema: &str) {
    let database_file_path = format!("{}/{}.db", path, schema);
    let _ = std::fs::remove_file(&database_file_path); // ignore potential errors
    std::thread::sleep(std::time::Duration::from_millis(100));
}
