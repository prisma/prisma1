#![allow(non_snake_case)]

use migration_connector::*;
use sql_migration_connector::SqlMigrationConnector;
use std::panic;
use std::path::Path;

#[test]
fn last_should_return_none_if_there_is_no_migration() {
    run_test(|| {
        let persistence = connector().migration_persistence();
        let result = persistence.last();
        assert_eq!(result.is_some(), false);
    });
}

#[test]
fn last_must_return_none_if_there_is_no_successful_migration() {
    run_test(|| {
        let persistence = connector().migration_persistence();
        persistence.create(Migration::new("my_migration".to_string()));
        let loaded = persistence.last();
        assert_eq!(loaded, None);
    });
}

#[test]
fn load_all_should_return_empty_if_there_is_no_migration() {
    run_test(|| {
        let persistence = connector().migration_persistence();
        let result = persistence.load_all();
        assert_eq!(result.is_empty(), true);
    });
}

#[test]
fn load_all_must_return_all_created_migrations() {
    run_test(|| {
        let persistence = connector().migration_persistence();
        let migration1 = persistence.create(Migration::new("migration_1".to_string()));
        let migration2 = persistence.create(Migration::new("migration_2".to_string()));
        let migration3 = persistence.create(Migration::new("migration_3".to_string()));

        let result = persistence.load_all();
        assert_eq!(result, vec![migration1, migration2, migration3])
    });
}

#[test]
fn create_should_allow_to_create_a_new_migration() {
    run_test(|| {
        let persistence = connector().migration_persistence();
        let mut migration = Migration::new("my_migration".to_string());
        migration.status = MigrationStatus::Success;
        let result = persistence.create(migration.clone());
        migration.revision = result.revision; // copy over the revision so that the assertion can work.`
        assert_eq!(result, migration);
        let loaded = persistence.last().unwrap();
        assert_eq!(loaded, migration);
    });
}

#[test]
fn create_should_increment_revisions() {
    run_test(|| {
        let persistence = connector().migration_persistence();
        let migration1 = persistence.create(Migration::new("migration_1".to_string()));
        let migration2 = persistence.create(Migration::new("migration_2".to_string()));
        assert_eq!(migration1.revision + 1, migration2.revision);
    });
}

#[test]
fn update_must_work() {
    run_test(|| {
        let persistence = connector().migration_persistence();
        let migration = persistence.create(Migration::new("my_migration".to_string()));

        let mut params = migration.update_params();
        params.status = MigrationStatus::Success;
        params.applied = 10;
        params.rolled_back = 11;
        params.errors = vec!["err1".to_string(), "err2".to_string()];
        params.finished_at = Some(Migration::timestamp_without_nanos());

        persistence.update(params.clone());

        let loaded = persistence.last().unwrap();
        assert_eq!(loaded.status, params.status);
        assert_eq!(loaded.applied, params.applied);
        assert_eq!(loaded.rolled_back, params.rolled_back);
        assert_eq!(loaded.errors, params.errors);
        assert_eq!(loaded.finished_at, params.finished_at);
    });
}


fn run_test<T>(test: T) -> ()
where
    T: FnOnce() -> () + panic::UnwindSafe,
{
    // SETUP
    let connector = connector();
    connector.initialize();
    connector.reset();

    // TEST
    let result = panic::catch_unwind(|| test());
    assert!(result.is_ok())
}

fn connector() -> Box<MigrationConnector<DatabaseMigrationStep = sql_migration_connector::SqlMigrationStep>> {
    let file_path = dbg!(file!());
    let file_name = dbg!(Path::new(file_path).file_stem().unwrap().to_str().unwrap());
    Box::new(SqlMigrationConnector::new(file_name.to_string()))
}