use super::QuerySchema;

/// Trait that should be implemented in order to be able to render a query schema.
pub trait QuerySchemaRenderer {
    fn render(query_schema: &QuerySchema) -> String;
}
