use migration_connector::*;
use std::sync::Arc;
use crate::commands::{CommandResult, CommandError};
use sql_migration_connector::{SqlMigrationConnector, SqlFamily};

pub fn load_connector(config: &str, must_exist: bool) -> CommandResult<Arc<MigrationConnector<DatabaseMigration = impl DatabaseMigrationMarker>>> {
    let sources = datamodel::load_data_source_configuration(config)?;
    let source = sources.first().ok_or(CommandError::DataModelErrors{code: 1000, errors: vec![
        "There is no datasource in the configuration.".to_string()
    ]})?;
    let sql_family = match source.connector_type().as_ref() {
        "sqlite" => SqlFamily::Sqlite,
        "postgres" => SqlFamily::Postgres,
        x => unimplemented!("Connector {} is not supported yet", x)
    };
    let url = source.url();
    let exists = SqlMigrationConnector::exists(sql_family, &url);
    match dbg!((exists, must_exist)) {
        (true, _) => Ok(SqlMigrationConnector::new(sql_family, &url)),
        (false, false) => Ok(SqlMigrationConnector::virtual_variant(sql_family, &url)),
        (false, true) => Ok(SqlMigrationConnector::new(sql_family, &url)), // this is only right for SQLite
    }
}