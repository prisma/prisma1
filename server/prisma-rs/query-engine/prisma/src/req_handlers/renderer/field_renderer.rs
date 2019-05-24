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
        let rendered_args = if rendered_args.len() == 0 {
            "".into()
        } else if rendered_args.len() > 1 {
            // Multiline - double indent.
            format!(
                "{}\n{}",
                rendered_args
                    .into_iter()
                    .map(|arg| format!("\n{}{}", ctx.indent().repeat(2), arg))
                    .collect::<Vec<String>>()
                    .join(""),
                ctx.indent()
            )
        } else {
            // Single line
            rendered_args.join(", ")
        };

        let (rendered_type, ctx) = field.field_type.into_renderer().render(ctx);
        (format!("{}({}): {}", field.name, rendered_args, rendered_type), ctx)
    }

    fn render_arguments(&self, args: &Vec<Argument>, ctx: RenderContext) -> (Vec<String>, RenderContext) {
        args.iter().fold((vec![], ctx), |(mut prev, ctx), arg| {
            let (rendered, ctx) = self.render_argument(arg, ctx);

            prev.push(rendered);
            (prev, ctx)
        })
    }

    fn render_argument(&self, arg: &Argument, ctx: RenderContext) -> (String, RenderContext) {
        let (rendered_type, ctx) = (&arg.argument_type).into_renderer().render(ctx);
        (format!("{}: {}", arg.name, rendered_type), ctx)
    }
}
