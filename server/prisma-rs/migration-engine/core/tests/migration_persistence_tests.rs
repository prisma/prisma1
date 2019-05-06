#![allow(non_snake_case)]

use migration_connector::*;
use sql_migration_connector::SqlMigrationConnector;
use std::sync::Arc;
use std::panic;

#[test]
fn last_should_return_none_if_there_is_no_migration() {
    run_test(||{
        let persistence = load_persistence();
        let result = persistence.last();
        assert_eq!(result.is_some(), false);
    });
}

#[test]
fn load_all_should_return_empty_if_there_is_no_migration(){
    run_test(||{
        let persistence = load_persistence();
        let result = persistence.load_all();
        assert_eq!(result.is_empty(), true);
    });
}

#[test]
fn create_should_allow_to_create_a_new_migration() {
    run_test(||{
        let persistence = load_persistence();
        let mut migration = Migration::new("my_migration".to_string());
        migration.status = MigrationStatus::Success;
        let result = persistence.create(migration.clone());
        assert_eq!(result, migration);
        let loaded = persistence.last().unwrap();
        assert_eq!(loaded, migration);
    });
}

fn load_persistence() -> Arc<MigrationPersistence> {
    let connector = SqlMigrationConnector::new();
    connector.migration_persistence()
}

fn run_test<T>(test: T) -> ()
    where T: FnOnce() -> () + panic::UnwindSafe
{
    // setup();
    let connector = SqlMigrationConnector::new();
    connector.initialize();
    connector.reset();
    let result = panic::catch_unwind(|| {
        test()
    });

    // teardown();

    assert!(result.is_ok())
}