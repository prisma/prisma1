use super::*;

pub enum GqlFieldRenderer<'a> {
    Input(&'a InputField),
    Output(&'a Field),
}

impl<'a> Renderer for GqlFieldRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext) {
        match &self {
            GqlFieldRenderer::Input(input) => self.render_input_field(input, ctx),
            GqlFieldRenderer::Output(output) => self.render_output_field(output, ctx),
        }
    }
}

impl<'a> GqlFieldRenderer<'a> {
    fn render_input_field(&self, input_field: &InputField, ctx: RenderContext) -> (String, RenderContext) {
        let (rendered_type, ctx) = (&input_field.field_type).into_renderer().render(ctx);

        (format!("{}: {}", input_field.name, rendered_type), ctx)
    }

    fn render_output_field(&self, field: &Field, ctx: RenderContext) -> (String, RenderContext) {
        let (rendered_args, ctx) = self.render_arguments(&field.arguments, ctx);
        let (rendered_type, ctx) = field.field_type.into_renderer().render(ctx);

        (format!("{}({}): {}", field.name, rendered_args, rendered_type), ctx)
    }

    fn render_arguments(&self, args: &Vec<Argument>, ctx: RenderContext) -> (String, RenderContext) {
        let (rendered, ctx): (Vec<String>, RenderContext) = args.iter().fold((vec![], ctx), |(mut prev, ctx), arg| {
            let (rendered, ctx) = self.render_argument(arg, ctx);

            prev.push(rendered);
            (prev, ctx)
        });

        if args.len() == 0 {
            ("".into(), ctx)
        } else if args.len() > 1 {
            // Multiline
            (format!("\n{}\n", rendered.join("\n")), ctx)
        } else {
            // Single line
            (rendered.join(", "), ctx)
        }
    }

    fn render_argument(&self, arg: &Argument, ctx: RenderContext) -> (String, RenderContext) {
        let (rendered_type, ctx) = (&arg.argument_type).into_renderer().render(ctx);
        (format!("{}: {}", arg.name, rendered_type), ctx)
    }
}
