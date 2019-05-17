use datamodel;
#[allow(dead_code)]
use migration_connector::*;
use migration_core::MigrationEngine;
use sql_migration_connector::SqlMigrationConnector;
use std::panic;
use std::path::Path;

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

pub fn run_test<T>(test: T) -> ()
where
    T: FnOnce() -> () + panic::UnwindSafe,
{
    run_test_with_engine(|_| test());
}

pub fn run_test_with_engine<T>(test: T) -> ()
where
    T: FnOnce(Box<MigrationEngine>) -> () + panic::UnwindSafe,
{
    // SETUP
    let engine = MigrationEngine::new();
    let connector = engine.connector();
    connector.initialize();
    connector.reset();

    // TEST
    let result = panic::catch_unwind(|| test(engine));
    assert!(result.is_ok())
}

// TODO: swap this out with connector loader and do not hard code associated type
pub fn connector() -> Box<MigrationConnector<DatabaseMigrationStep = impl DatabaseMigrationStepExt>> {
    let file_path = dbg!(file!());
    let file_name = dbg!(Path::new(file_path).file_stem().unwrap().to_str().unwrap());
    Box::new(SqlMigrationConnector::new(file_name.to_string()))
}
