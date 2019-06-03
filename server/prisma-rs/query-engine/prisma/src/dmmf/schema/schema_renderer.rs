use super::*;

pub struct DMMFSchemaRenderer<'schema> {
  query_schema: &'schema QuerySchema,
}

impl<'schema> Renderer<'schema, ()> for DMMFSchemaRenderer<'schema> {
  fn render(&self, ctx: RenderContext) -> ((), RenderContext) {
    let (_, ctx) = self.query_schema.query.into_renderer().render(ctx);
    let (_, ctx) = self.query_schema.mutation.into_renderer().render(ctx);

    ((), ctx)
  }
}

impl<'schema> DMMFSchemaRenderer<'schema> {
  pub fn new(query_schema: &'schema QuerySchema) -> DMMFSchemaRenderer<'schema> {
    DMMFSchemaRenderer { query_schema }
  }
}
