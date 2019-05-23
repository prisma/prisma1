use super::*;

/// Top level GraphQL schema renderer.
pub struct GqlSchemaRenderer<'schema> {
    query_schema: &'schema QuerySchema,
}

impl<'schema> Renderer for GqlSchemaRenderer<'schema> {
    fn render(&self, ctx: RenderContext) -> RenderContext {
        // self.render_type(&query_schema.query);
        // self.render_type(&query_schema.mutation);

        unimplemented!()
    }
}

impl<'schema> GqlSchemaRenderer<'schema> {
    pub fn new(query_schema: &'schema QuerySchema) -> GqlSchemaRenderer<'schema> {
        GqlSchemaRenderer { query_schema }
    }
}
