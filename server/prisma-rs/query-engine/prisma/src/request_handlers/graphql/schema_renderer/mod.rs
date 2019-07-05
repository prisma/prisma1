mod enum_renderer;
mod field_renderer;
mod object_renderer;
mod schema_renderer;
mod type_renderer;

use core::schema::*;
use enum_renderer::*;
use field_renderer::*;
use object_renderer::*;
use schema_renderer::*;
use std::{
    cell::RefCell,
    collections::HashMap,
    sync::{Arc, Weak},
};
use type_renderer::*;

#[allow(dead_code)]
pub struct GraphQLSchemaRenderer;

impl QuerySchemaRenderer<String> for GraphQLSchemaRenderer {
    fn render(query_schema: QuerySchemaRef) -> String {
        let context = RenderContext::new();
        let (_, result) = query_schema.into_renderer().render(context);

        // Add custom scalar types (required for graphql.js implementations)
        format!("{}\n\nscalar DateTime\nscalar Json\nscalar UUID", result.format())
    }
}

pub trait Renderer {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext);
}

pub struct RenderContext {
    /// Output queue for all (top level) elements that need to be rendered,
    output_queue: RefCell<Vec<String>>,

    /// Prevents double rendering of elements that are referenced multiple times.
    rendered: RefCell<HashMap<String, ()>>,

    /// General indent level in spaces.
    indent: usize,

    /// Indent string.
    indent_str: &'static str,
}

impl RenderContext {
    pub fn new() -> RenderContext {
        RenderContext {
            output_queue: RefCell::new(vec![]),
            rendered: RefCell::new(HashMap::new()),
            indent: 2,
            indent_str: " ",
        }
    }

    pub fn format(self) -> String {
        self.output_queue.borrow().join("\n\n")
    }

    pub fn already_rendered(&self, cache_key: &str) -> bool {
        self.rendered.borrow().contains_key(cache_key)
    }

    pub fn mark_as_rendered(&self, cache_key: String) {
        self.rendered.borrow_mut().insert(cache_key, ());
    }

    pub fn add_output(&self, output: String) {
        self.output_queue.borrow_mut().push(output);
    }

    pub fn add(&self, cache_key: String, output: String) {
        self.add_output(output);
        self.mark_as_rendered(cache_key);
    }

    pub fn indent(&self) -> String {
        self.indent_str.repeat(self.indent)
    }
}

enum GqlRenderer<'a> {
    Schema(GqlSchemaRenderer),
    Object(GqlObjectRenderer),
    Type(GqlTypeRenderer<'a>),
    Field(GqlFieldRenderer),
    Enum(GqlEnumRenderer<'a>),
}

impl<'a> Renderer for GqlRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext) {
        match self {
            GqlRenderer::Schema(s) => s.render(ctx),
            GqlRenderer::Object(o) => o.render(ctx),
            GqlRenderer::Type(t) => t.render(ctx),
            GqlRenderer::Field(f) => f.render(ctx),
            GqlRenderer::Enum(e) => e.render(ctx),
        }
    }
}

trait IntoRenderer<'a> {
    fn into_renderer(&'a self) -> GqlRenderer<'a>;
}

impl<'a> IntoRenderer<'a> for QuerySchemaRef {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Schema(GqlSchemaRenderer::new(Arc::clone(self)))
    }
}

impl<'a> IntoRenderer<'a> for &'a InputType {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Type(GqlTypeRenderer::Input(self))
    }
}

impl<'a> IntoRenderer<'a> for OutputType {
    fn into_renderer(&'a self) -> GqlRenderer<'a> {
        GqlRenderer::Type(GqlTypeRenderer::Output(self))
    }
}

impl<'a> IntoRenderer<'a> for InputFieldRef {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Field(GqlFieldRenderer::Input(Arc::clone(self)))
    }
}

impl<'a> IntoRenderer<'a> for FieldRef {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Field(GqlFieldRenderer::Output(Arc::clone(self)))
    }
}

impl<'a> IntoRenderer<'a> for EnumType {
    fn into_renderer(&'a self) -> GqlRenderer<'a> {
        GqlRenderer::Enum(GqlEnumRenderer::new(self))
    }
}

impl<'a> IntoRenderer<'a> for &'a InputObjectTypeRef {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Object(GqlObjectRenderer::Input(Weak::clone(self)))
    }
}

impl<'a> IntoRenderer<'a> for &'a ObjectTypeRef {
    fn into_renderer(&self) -> GqlRenderer<'a> {
        GqlRenderer::Object(GqlObjectRenderer::Output(Weak::clone(self)))
    }
}
