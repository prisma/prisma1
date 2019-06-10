

use database_inspector::*;
use datamodel;
use migration_core::MigrationEngine;
use std::panic;

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

pub fn run_test_with_engine<T, X>(test: T) -> X
where
    T: FnOnce(Box<MigrationEngine>) -> X + panic::UnwindSafe,
{
    // SETUP
    let engine = MigrationEngine::new();
    let connector = engine.connector();
    connector.reset();
    engine.init();

    // TEST
    let result = panic::catch_unwind(|| test(engine));
    assert!(result.is_ok());
    result.unwrap()
}

pub fn introspect_database(engine: &Box<MigrationEngine>) -> DatabaseSchema {
    let inspector = engine.connector().database_inspector();
    let mut result = inspector.introspect(&engine.schema_name());
    // the presence of the _Migration table makes assertions harder. Therefore remove it.
    result.tables = result.tables.into_iter().filter(|t| t.name != "_Migration").collect();
    result
}