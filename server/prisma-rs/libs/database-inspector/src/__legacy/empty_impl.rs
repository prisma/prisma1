use crate::*;

pub struct EmptyDatabaseInspectorImpl;
impl DatabaseInspector for EmptyDatabaseInspectorImpl {
    fn introspect(&self, _schema: &String) -> DatabaseSchema {
        DatabaseSchema { tables: Vec::new() }
    }
}
