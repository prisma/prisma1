#[allow(dead_code)]

use migration_connector::*;
use sql_migration_connector::SqlMigrationConnector;
use std::panic;
use std::path::Path;
use datamodel;

pub fn parse(datamodel_string: &str) -> datamodel::Schema {
    match datamodel::parse(datamodel_string) {
        Ok(s) => s,
        Err(errs) => { 
            for err in errs.to_iter() {
                err.pretty_print(&mut std::io::stderr().lock(), "", datamodel_string).unwrap();
            }
            panic!("Schema parsing failed. Please see error above.")
        }
    }
}

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
