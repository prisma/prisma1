#![allow(non_snake_case)]
mod test_harness;
use datamodel::dml::*;
use migration_connector::*;
use migration_core::api::GenericApi;
use migration_core::commands::*;
use sql_migration_connector::*;
use test_harness::*;

#[test]
fn must_be_true_when_it_can_connect() {
    test_it(SqlMigrationConnector::postgres(&postgres_url()).unwrap(), true);
    test_it(SqlMigrationConnector::mysql(&mysql_url()).unwrap(), true);
}

#[test]
#[ignore]
fn must_be_false_when_credentials_are_wrong() {
    let url = postgres_url().replace(":prisma", "foobar");
    test_it(SqlMigrationConnector::postgres(&url).unwrap(), false);
}

#[test]
#[ignore]
fn must_be_false_when_the_database_does_not_exist() {
    let url = postgres_url().replace("migration_engine", "foobar");
    test_it(SqlMigrationConnector::postgres(&url).unwrap(), false);
}

#[test]
#[ignore]
fn must_be_false_when_the_schema_does_not_exist() {
    let url = postgres_url().replace("migration_engine", "foobar");
    test_it(SqlMigrationConnector::postgres(&url).unwrap(), false);
}

#[test]
#[ignore]
fn must_be_false_when_the_host_does_not_exist() {
    let url = postgres_url().replace("5432", "5555");
    test_it(SqlMigrationConnector::postgres(&url).unwrap(), false);
}

fn test_it<C, D>(connector: C, expected: bool)
where
    C: MigrationConnector<DatabaseMigration = D>,
    D: DatabaseMigrationMarker + Send + Sync + 'static,
{
    let api = test_api(connector);
    assert_eq!(
        api.can_connect_to_database(&CanConnectToDatabaseInput {})
            .expect("CanConnect failed")
            .result,
        expected
    );
}
