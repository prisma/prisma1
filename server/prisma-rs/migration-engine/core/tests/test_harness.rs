use migration_connector::*;
use sql_migration_connector::SqlMigrationConnector;
use std::panic;
use std::path::Path;

pub fn run_test<T>(test: T) -> ()
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

// TODO: swap this out with connector loader and do not hard code associated type
pub fn connector() -> Box<MigrationConnector<DatabaseMigrationStep = impl DatabaseMigrationStepExt>> {
    let file_path = dbg!(file!());
    let file_name = dbg!(Path::new(file_path).file_stem().unwrap().to_str().unwrap());
    Box::new(SqlMigrationConnector::new(file_name.to_string()))
}
