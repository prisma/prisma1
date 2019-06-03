use super::{MySqlSource, MYSQL_SOURCE_NAME};
use crate::{common::argument::Arguments, errors::ValidationError, source::*};

pub struct MySqlSourceDefinition {}

impl MySqlSourceDefinition {
    pub fn new() -> MySqlSourceDefinition {
        MySqlSourceDefinition {}
    }
}

impl SourceDefinition for MySqlSourceDefinition {
    fn name(&self) -> &'static str {
        MYSQL_SOURCE_NAME
    }

    fn create(&self, name: &str, url: &str, _arguments: &Arguments) -> Result<Box<Source>, ValidationError> {
        Ok(Box::new(MySqlSource {
            name: String::from(name),
            url: String::from(url),
        }))
    }
}
