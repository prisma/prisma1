use super::*;

/// Top level GraphQL schema renderer.
pub struct GqlSchemaRenderer {
    query_schema: QuerySchemaRef,
}

impl Renderer for GqlSchemaRenderer {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext) {
        let (_, ctx) = self.query_schema.query.into_renderer().render(ctx);
        let result = self.query_schema.mutation.into_renderer().render(ctx);

        result
    }
}

impl GqlSchemaRenderer {
    pub fn new(query_schema: QuerySchemaRef) -> GqlSchemaRenderer {
        GqlSchemaRenderer { query_schema }
    }
}
