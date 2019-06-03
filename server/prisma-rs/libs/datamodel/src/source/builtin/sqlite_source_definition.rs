use super::{SqliteSource, SQLITE_SOURCE_NAME};
use crate::{common::argument::Arguments, errors::ValidationError, source::*};

pub struct SqliteSourceDefinition {}

impl SqliteSourceDefinition {
    pub fn new() -> SqliteSourceDefinition {
        SqliteSourceDefinition {}
    }
}

impl SourceDefinition for SqliteSourceDefinition {
    fn name(&self) -> &'static str {
        SQLITE_SOURCE_NAME
    }

    fn create(&self, name: &str, url: &str, _arguments: &Arguments) -> Result<Box<Source>, ValidationError> {
        Ok(Box::new(SqliteSource {
            name: String::from(name),
            url: String::from(url),
        }))
    }
}
