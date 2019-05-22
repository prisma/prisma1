use core::schema::*;
use std::collections::HashMap;

struct GqlSchemaRenderer {
  // Output queue for all top level elements that need to be rendered,
  output_queue: Vec<String>,

  // Prevents double rendering of elements that are references multiple times.
  rendered: HashMap<String, ()>,
}

impl QuerySchemaRenderer for GqlSchemaRenderer {
  fn render(&mut self, query_schema: &QuerySchema) -> String {
    self.render_type(&query_schema.query);

    unimplemented!()
  }
}

impl GqlSchemaRenderer {
  pub fn new() -> GqlSchemaRenderer {
    GqlSchemaRenderer {
      output_queue: vec![],
      rendered: HashMap::new(),
    }
  }

  fn render_type(&mut self, typ: &ObjectType) {
    unimplemented!()
  }

  fn render_fields(&mut self, field: &Field) -> Vec<String> {
    vec![]
  }
}
