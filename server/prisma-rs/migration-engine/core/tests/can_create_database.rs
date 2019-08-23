#![allow(non_snake_case)]
mod test_harness;
use datamodel::dml::*;
use migration_connector::*;
use migration_core::api::GenericApi;
use migration_core::commands::*;
use sql_migration_connector::*;
use test_harness::*;

#[test]
#[ignore]
fn must_be_true_when_the_user_has_the_rights() {
    let url = postgres_url();
    test_it(SqlMigrationConnector::postgres(&url).unwrap(), true);
}

#[test]
#[ignore] // we currently have no way to setup a user with limited rights
fn must_be_false_when_the_user_does_not_have_the_rights() {
    let url = postgres_url();
    test_it(SqlMigrationConnector::postgres(&url).unwrap(), false);
}

fn test_it<C, D>(connector: C, expected: bool)
where
    C: MigrationConnector<DatabaseMigration = D>,
    D: DatabaseMigrationMarker + Send + Sync + 'static,
{
    let api = test_api(connector);
    assert_eq!(
        api.can_create_database(&CanCreateDatabaseInput {})
            .expect("CanCreateDatabase failed")
            .result,
        expected
    );
}
