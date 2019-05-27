use datamodel;
#[allow(dead_code)]
use migration_core::MigrationEngine;
use std::panic;
use sql_migration_connector::{SqlMigrationConnector, SqlMigrationStep};
use prisma_query::{connector::*, transaction::{Connection, Connectional}};

pub fn parse(datamodel_string: &str) -> datamodel::Schema {
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

const SCHEMA: &str = "migration_tests";

pub fn run_test_with_engine<T>(test: T) -> ()
where
    T: FnOnce(Box<MigrationEngine<SqlMigrationStep>>, &std::cell::RefCell<Connection>) -> () + panic::UnwindSafe,
{
    // CONNECTION SETUP
    let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
    let path = format!("{}/db", server_root);
    remove_db(&path, SCHEMA);
    
    let client = Sqlite::new(path, 32, false).unwrap();

    client.with_shared_connection(SCHEMA, |connection| {
        // SETUP

        let connector = SqlMigrationConnector::new(SCHEMA, connection);
        let engine = MigrationEngine::<SqlMigrationStep>::new(&connector);
        let connector = engine.connector();
        connector.reset();
        connector.initialize();

        // TEST
        test(Box::new(engine), connection);

        Ok(())
    });
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