use crate::commands::{CommandError, CommandResult};
use migration_connector::*;
use sql_migration_connector::{SqlFamily, SqlMigrationConnector};
use std::sync::Arc;

pub fn load_connector(
    config: &str,
    must_exist: bool,
) -> CommandResult<Arc<MigrationConnector<DatabaseMigration = impl DatabaseMigrationMarker>>> {
    let config = datamodel::load_configuration(config)?;
    let source = config.datasources.first().ok_or(CommandError::DataModelErrors {
        code: 1000,
        errors: vec!["There is no datasource in the configuration.".to_string()],
    })?;
    let sql_family = match source.connector_type().as_ref() {
        "sqlite" => SqlFamily::Sqlite,
        "postgresql" => SqlFamily::Postgres,
        "mysql" => SqlFamily::Mysql,
        x => unimplemented!("Connector {} is not supported yet", x),
    };
    let url = &source.url().value;
    let exists = SqlMigrationConnector::exists(sql_family, &url);
    match (exists, must_exist) {
        (true, _) => Ok(SqlMigrationConnector::new(sql_family, &url)),
        (false, false) => Ok(SqlMigrationConnector::virtual_variant(sql_family, &url)),
        (false, true) => Ok(SqlMigrationConnector::new(sql_family, &url)), // this is only right for SQLite
    }
}
