use super::*;

/// Top level GraphQL schema renderer.
pub struct GqlSchemaRenderer<'schema> {
    query_schema: &'schema QuerySchema,
}

impl<'schema> Renderer for GqlSchemaRenderer<'schema> {
    fn render(&self, ctx: RenderContext) -> RenderContext {
        let queries = self.query_schema.query.into_renderer().render(ctx);
        let all = self.query_schema.mutation.into_renderer().render(queries);

        all
    }
}

impl<'schema> GqlSchemaRenderer<'schema> {
    pub fn new(query_schema: &'schema QuerySchema) -> GqlSchemaRenderer<'schema> {
        GqlSchemaRenderer { query_schema }
    }
}
