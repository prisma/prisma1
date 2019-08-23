use super::{SqliteSource, SQLITE_SOURCE_NAME};
use crate::{common::argument::Arguments, configuration::*, errors::ValidationError};

#[derive(Default)]
pub struct SqliteSourceDefinition {}

impl SqliteSourceDefinition {
    pub fn new() -> Self {
        Self::default()
    }
}

impl SourceDefinition for SqliteSourceDefinition {
    fn connector_type(&self) -> &'static str {
        SQLITE_SOURCE_NAME
    }

    fn create(
        &self,
        name: &str,
        url: StringFromEnvVar,
        _arguments: &mut Arguments,
        documentation: &Option<String>,
    ) -> Result<Box<dyn Source>, ValidationError> {
        Ok(Box::new(SqliteSource {
            name: String::from(name),
            url: url,
            documentation: documentation.clone(),
        }))
    }
}
