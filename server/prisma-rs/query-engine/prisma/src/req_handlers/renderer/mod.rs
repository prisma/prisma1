use core::schema::*;
use std::{cell::RefCell, collections::HashMap};

mod enum_renderer;
mod field_renderer;
mod schema_renderer;
mod type_renderer;

use enum_renderer::*;
use field_renderer::*;
use schema_renderer::*;
use type_renderer::*;

pub struct RootRenderer;

impl QuerySchemaRenderer for RootRenderer {
    fn render(&self, query_schema: &QuerySchema) -> String {
        let context = RenderContext::new();
        let result = query_schema.into_renderer().render(context);

        result.format()
    }
}

pub trait Renderer {
    fn render(&self, ctx: RenderContext) -> RenderContext;
}

pub struct RenderContext {
    /// Output queue for all (top level) elements that need to be rendered,
    output_queue: RefCell<Vec<String>>,

    /// Prevents double rendering of elements that are referenced multiple times.
    rendered: RefCell<HashMap<String, ()>>,

    /// General indent level in spaces.
    indent: usize,
}

impl RenderContext {
    pub fn new() -> RenderContext {
        RenderContext {
            output_queue: RefCell::new(vec![]),
            rendered: RefCell::new(HashMap::new()),
            indent: 4,
        }
    }

    pub fn format(self) -> String {
        self.output_queue.borrow().join("\n")
    }

    pub fn should_render(&self, cache_key: &str) -> bool {
        self.rendered.borrow().contains_key(cache_key)
    }

    pub fn add(&self, cache_key: String, output: String) {
        self.output_queue.borrow_mut().push(output);
        self.rendered
            .borrow_mut()
            .insert(cache_key, ())
            .expect("Expected render caching operation to always suceed/");
    }
}

enum GqlRenderer<'a> {
    Schema(GqlSchemaRenderer<'a>),
    Type(GqlTypeRenderer<'a>),
    Field(GqlFieldRenderer<'a>),
    Enum(GqlEnumRenderer),
}

impl<'a> Renderer for GqlRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> RenderContext {
        match self {
            GqlRenderer::Schema(s) => s.render(ctx),
            GqlRenderer::Type(t) => t.render(ctx),
            GqlRenderer::Field(f) => f.render(ctx),
            GqlRenderer::Enum(e) => e.render(ctx),
        }
    }
}

trait IntoRenderer<'a> {
    fn into_renderer(&self) -> GqlRenderer<'a>;
}

impl<'a> IntoRenderer<'a> for &'a QuerySchema {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Schema(GqlSchemaRenderer::new(self))
    }
}

impl<'a> IntoRenderer<'a> for &'a InputType {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Type(GqlTypeRenderer::Input(self))
    }
}

impl<'a> IntoRenderer<'a> for &'a OutputType {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Type(GqlTypeRenderer::Output(self))
    }
}

impl<'a> IntoRenderer<'a> for &'a InputField {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Field(GqlFieldRenderer::Input(self))
    }
}

impl<'a> IntoRenderer<'a> for &'a Field {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Field(GqlFieldRenderer::Output(self))
    }
}
