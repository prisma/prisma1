use super::*;

pub struct EmptyDatabaseInspectorImpl;
impl DatabaseInspector for EmptyDatabaseInspectorImpl {
    fn introspect(&self, _schema: &String) -> DatabaseSchemaOld {
        DatabaseSchemaOld { tables: Vec::new() }
    }
}
