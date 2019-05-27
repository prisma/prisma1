use crate::*;
use super::*;
pub trait SpecializedRelationalIntrospectionResult {
    fn database_type(&self) -> &str;
    /*
    // TODO: This should map a DB type to DML.
    fn to_type_identifier(&self, field_type: &str) -> String;
    // TODO
    fn parse_default_value(&self, default_value_string: &str, field_type: &str) -> String;
    // TODO: should map from a DML type.
    fn is_type_reserved(&self, field_type: &str) -> bool;
    // TODO:
    fn resolve_sequences(&self) -> &String;
    */
}

pub struct RelationalIntrospectionResult {
    /// Any specialized connector specific results.
    pub specialized: Box<SpecializedRelationalIntrospectionResult>,
    /// The relational database schema without prisma abstractions.
    pub schema: SchemaInfo
}

// TODO: This should follow the RelationalIntrospectionResult class
// from the TS implementation.
impl IntrospectionResult for RelationalIntrospectionResult {
    fn database_type(&self) -> &str {
        self.specialized.database_type()
    }
}
