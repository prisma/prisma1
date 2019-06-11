use super::{SqliteSource, SQLITE_SOURCE_NAME};
use crate::{common::argument::Arguments, errors::ValidationError, configuration::*};

pub struct SqliteSourceDefinition {}

impl SqliteSourceDefinition {
    pub fn new() -> SqliteSourceDefinition {
        SqliteSourceDefinition {}
    }
}

impl SourceDefinition for SqliteSourceDefinition {
    fn connector_type(&self) -> &'static str {
        SQLITE_SOURCE_NAME
    }

    fn create(
        &self,
        name: &str,
        url: &str,
        _arguments: &Arguments,
        documentation: &Option<String>,
    ) -> Result<Box<Source>, ValidationError> {
        Ok(Box::new(SqliteSource {
            name: String::from(name),
            url: String::from(url),
            documentation: documentation.clone(),
        }))
    }
}
