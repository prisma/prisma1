use super::*;

pub struct DMMFSchemaRenderer {
    query_schema: QuerySchemaRef,
}

impl<'a> Renderer<'a, ()> for DMMFSchemaRenderer {
    fn render(&self, ctx: RenderContext) -> ((), RenderContext) {
        let (_, ctx) = self.query_schema.query.into_renderer().render(ctx);
        let (_, ctx) = self.query_schema.mutation.into_renderer().render(ctx);

        ((), ctx)
    }
}

impl DMMFSchemaRenderer {
    pub fn new(query_schema: QuerySchemaRef) -> DMMFSchemaRenderer {
        DMMFSchemaRenderer { query_schema }
    }
}
