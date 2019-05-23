use crate::{Connection, SqlError};
use datamodel::dml;

/// This traits are for future use.
///
/// It does not make sense to implement them as long as we don't
/// have normalization or rendering in place.
///
/// We use two traits because of the learnings from the ts-implementation:
/// It's useful to distinguish between a raw-introspection result and
/// the actual datamodel representation.

/// Represents a database connector which is capable of introspecting a database.
pub trait IntrospectionConnector<ResultType: IntrospectionResult> {
    fn database_type(&self) -> &str;
    fn list_schemas(&self, connection: &mut Connection) -> Result<Vec<String>, SqlError>;
    fn introspect(&self, connection: &mut Connection, schema: &str) -> Result<ResultType, SqlError>;
}

/// Represents the raw result of an introspection, which can be converted into a datamodel.
pub trait IntrospectionResult {
    fn database_type(&self) -> &str;
    fn get_datamodel(&self) -> dml::Schema;
    fn get_normalized_datamodel(&self) -> dml::Schema;
    fn render_datamodel(&self) -> String;
    fn render_normalized_datamodel(&self) -> String;
}
