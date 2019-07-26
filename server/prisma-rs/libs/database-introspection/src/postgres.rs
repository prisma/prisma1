use super::*;
use log::debug;
use ::postgres::Client;

pub struct IntrospectionConnector {
    client: Client,
}

impl super::IntrospectionConnector for IntrospectionConnector {
    fn list_schemas(&self) -> Result<Vec<String>> {
        Ok(vec![])
    }

    fn introspect(&mut self, schema: &str) -> Result<DatabaseSchema> {
        debug!("Introspecting schema '{}'", schema);
        Ok(DatabaseSchema {
            enums: vec![],
            sequences: vec![],
            tables: vec![],
        })
    }
}

impl IntrospectionConnector {
    pub fn new(client: Client) -> Result<IntrospectionConnector> {
        Ok(IntrospectionConnector{
            client,
        })
    }
}
