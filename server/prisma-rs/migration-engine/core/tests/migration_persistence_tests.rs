#![allow(non_snake_case)]

mod test_harness;

use migration_connector::*;
use test_harness::*;

#[test]
fn last_should_return_none_if_there_is_no_migration() {
    run_test_with_engine(|engine, _| {
        let persistence = engine.connector().migration_persistence();
        let result = persistence.last();
        assert_eq!(result.is_ok(), false);
    });
}

#[test]
fn last_must_return_none_if_there_is_no_successful_migration() {
    run_test_with_engine(|engine, _| {
        let persistence = engine.connector().migration_persistence();
        persistence.create(Migration::new("my_migration".to_string()));
        let loaded = persistence.last();
        assert_eq!(loaded.is_ok(), false);
    });
}

#[test]
fn load_all_should_return_empty_if_there_is_no_migration() {
    run_test_with_engine(|engine, _| {
        let persistence = engine.connector().migration_persistence();
        let result = persistence.load_all();
        assert_eq!(result.is_err(), true);
    });
}

#[test]
fn load_all_must_return_all_created_migrations() {
    run_test_with_engine(|engine, _| {
        let persistence = engine.connector().migration_persistence();
        let migration1 = persistence.create(Migration::new("migration_1".to_string())).unwrap();
        let migration2 = persistence.create(Migration::new("migration_2".to_string())).unwrap();
        let migration3 = persistence.create(Migration::new("migration_3".to_string())).unwrap();

        let result = persistence.load_all().unwrap();
        assert_eq!(result, vec![migration1, migration2, migration3]);
    });
}

#[test]
fn create_should_allow_to_create_a_new_migration() {
    run_test_with_engine(|engine, _| {
        let persistence = engine.connector().migration_persistence();
        let mut migration = Migration::new("my_migration".to_string());
        migration.status = MigrationStatus::Success;
        let result = persistence.create(migration.clone()).unwrap();
        migration.revision = result.revision; // copy over the revision so that the assertion can work.`
        assert_eq!(result, migration);
        let loaded = persistence.last().unwrap();
        assert_eq!(loaded, migration);
    });
}

#[test]
fn create_should_increment_revisions() {
    run_test_with_engine(|engine, _| {
        let persistence = engine.connector().migration_persistence();
        let migration1 = persistence.create(Migration::new("migration_1".to_string())).unwrap();
        let migration2 = persistence.create(Migration::new("migration_2".to_string())).unwrap();
        assert_eq!(migration1.revision + 1, migration2.revision);
    });
}

#[test]
fn update_must_work() {
    run_test_with_engine(|engine, _| {
        let persistence = engine.connector().migration_persistence();
        let migration = persistence.create(Migration::new("my_migration".to_string())).unwrap();

        let mut params = migration.update_params();
        params.status = MigrationStatus::Success;
        params.applied = 10;
        params.rolled_back = 11;
        params.errors = vec!["err1".to_string(), "err2".to_string()];
        params.finished_at = Some(Migration::timestamp_without_nanos());

        persistence.update(&params);

        let loaded = persistence.last().unwrap();
        assert_eq!(loaded.status, params.status);
        assert_eq!(loaded.applied, params.applied);
        assert_eq!(loaded.rolled_back, params.rolled_back);
        assert_eq!(loaded.errors, params.errors);
        assert_eq!(loaded.finished_at, params.finished_at);
    });
}
