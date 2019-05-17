use datamodel;
#[allow(dead_code)]
use migration_core::MigrationEngine;
use std::panic;

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

pub fn run_test_with_engine<T>(test: T) -> ()
where
    T: FnOnce(Box<MigrationEngine>) -> () + panic::UnwindSafe,
{
    // SETUP
    let engine = MigrationEngine::new();
    let connector = engine.connector();
    connector.reset();
    connector.initialize();

    // TEST
    let result = panic::catch_unwind(|| test(engine));
    assert!(result.is_ok())
}
