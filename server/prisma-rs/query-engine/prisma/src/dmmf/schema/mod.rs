mod ast;

pub use ast::*;

use core::schema::*;
use std::{cell::RefCell, collections::HashMap};

struct DMMFRenderer;

impl QuerySchemaRenderer<DMMFSchema> for DMMFRenderer {
  fn render(query_schema: &QuerySchema) -> DMMFSchema {
    unimplemented!()
  }
}

pub struct RenderContext {
  /// Aggregator
  result: DMMFSchema,

  /// Prevents double rendering of elements that are referenced multiple times.
  /// Names of input / output types / enums / models are globally unique.
  rendered: RefCell<HashMap<String, ()>>,
}

impl RenderContext {
  pub fn new() -> Self {
    RenderContext {
      result: DMMFSchema::new(),
      rendered: RefCell::new(HashMap::new()),
    }
  }
}
