use migration_connector::*;
use std::sync::Arc;
use crate::commands::{CommandResult, CommandError};
use sql_migration_connector::{SqlMigrationConnector, SqlFamily};

pub fn load_connector(config: &str) -> CommandResult<Arc<MigrationConnector<DatabaseMigration = impl DatabaseMigrationMarker>>> {
    let sources = datamodel::load_data_source_configuration(config)?;
    let source = sources.first().ok_or(CommandError::DataModelErrors{code: 1000, errors: vec![
        "There is no datasource in the configuration.".to_string()
    ]})?;
    match source.connector_type().as_ref() {
        "sqlite" => {
            let url = source.url();
            Ok(Arc::new(SqlMigrationConnector::new(SqlFamily::Sqlite, &url)))
        },
        x => unimplemented!("Connector {} is not supported yet", x)
    }
}