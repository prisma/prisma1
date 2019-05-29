use super::*;

pub enum GqlTypeRenderer<'a> {
    Input(&'a InputType),
    Output(&'a OutputType),
}

impl<'a> Renderer for GqlTypeRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext) {
        match self {
            GqlTypeRenderer::Input(i) => self.render_input_type(i, ctx),
            GqlTypeRenderer::Output(o) => self.render_output_type(o, ctx),
        }
    }
}

/// How rendering types is done:
/// - Render representations recursively (e.g. [TestInputType] would be Option(List(InputType))).
/// - During that, render dependent types as well by calling the correct renderer, which will add the dependent type to the output queue.
///
/// Important to note: The way the AST is build, it's easier to remove "!" (required) suffixes instead of adding them, which is why you
/// will see types always appending "!" until optional removes them.
impl<'a> GqlTypeRenderer<'a> {
    fn render_input_type(&self, i: &InputType, ctx: RenderContext) -> (String, RenderContext) {
        match i {
            InputType::Object(ref obj) => {
                let (_, subctx) = obj.into_renderer().render(ctx);
                (format!("{}!", obj.into_arc().name), subctx)
            }
            InputType::Enum(et) => {
                // Not sure how this fits together with the enum handling below.
                let (_, subctx) = et.into_renderer().render(ctx);
                (format!("{}!", et.name), subctx)
            }
            InputType::List(ref l) => {
                let (substring, subctx) = self.render_input_type(l, ctx);
                (format!("[{}]!", substring), subctx)
            }
            InputType::Opt(ref opt) => {
                let (substring, subctx) = self.render_input_type(opt, ctx);
                (substring.trim_end_matches("!").to_owned(), subctx)
            }
            InputType::Scalar(ScalarType::Enum(et)) => {
                let (_, subctx) = et.into_renderer().render(ctx);
                (format!("{}!", et.name), subctx)
            }
            InputType::Scalar(ref scalar) => {
                let stringified = match scalar {
                    ScalarType::String => "String",
                    ScalarType::Int => "Int",
                    ScalarType::Boolean => "Boolean",
                    ScalarType::Float => "Float",
                    ScalarType::DateTime => "DateTime",
                    ScalarType::Json => "DateTime",
                    ScalarType::ID => "ID",
                    ScalarType::UUID => "UUID",
                    ScalarType::Enum(_) => unreachable!(), // Handled separately above.
                };

                (format!("{}!", stringified), ctx)
            }
        }
    }

    fn render_output_type(&self, o: &OutputType, ctx: RenderContext) -> (String, RenderContext) {
        match o {
            OutputType::Object(obj) => {
                let (_, subctx) = obj.into_renderer().render(ctx);
                (format!("{}!", obj.into_arc().name), subctx)
            }
            OutputType::Enum(et) => {
                // Not sure how this fits together with the enum handling below.
                let (_, subctx) = et.into_renderer().render(ctx);
                (format!("{}!", et.name), subctx)
            }
            OutputType::List(l) => {
                let (substring, subctx) = self.render_output_type(l, ctx);
                (format!("[{}]!", substring), subctx)
            }
            OutputType::Opt(ref opt) => {
                let (substring, subctx) = self.render_output_type(opt, ctx);
                (substring.trim_end_matches("!").to_owned(), subctx)
            }
            OutputType::Scalar(ScalarType::Enum(et)) => {
                let (_, subctx) = et.into_renderer().render(ctx);
                (format!("{}!", et.name), subctx)
            }
            OutputType::Scalar(ref scalar) => {
                let stringified = match scalar {
                    ScalarType::String => "String",
                    ScalarType::Int => "Int",
                    ScalarType::Boolean => "Boolean",
                    ScalarType::Float => "Float",
                    ScalarType::DateTime => "DateTime",
                    ScalarType::Json => "DateTime",
                    ScalarType::ID => "ID",
                    ScalarType::UUID => "UUID",
                    ScalarType::Enum(_) => unreachable!(), // Handled separately above.
                };

                (format!("{}!", stringified), ctx)
            }
        }
    }
}
