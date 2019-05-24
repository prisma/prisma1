use super::*;

#[derive(Debug)]
pub enum GqlObjectRenderer {
    Input(InputObjectTypeRef),
    Output(ObjectTypeRef),
}

impl Renderer for GqlObjectRenderer {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext) {
        match &self {
            GqlObjectRenderer::Input(input) => self.render_input_object(input, ctx),
            GqlObjectRenderer::Output(output) => self.render_output_object(output, ctx),
        }
    }
}

impl GqlObjectRenderer {
    fn render_input_object(&self, input_object: &InputObjectTypeRef, ctx: RenderContext) -> (String, RenderContext) {
        if ctx.already_rendered(&input_object.name) {
            return ("".into(), ctx);
        } else {
            // This short circuits recursive processing for fields.
            ctx.mark_as_rendered(input_object.name.clone())
        }

        let (rendered_fields, ctx): (Vec<String>, RenderContext) =
            input_object
                .get_fields()
                .iter()
                .fold((vec![], ctx), |(mut acc, ctx), field| {
                    let (rendered_field, ctx) = field.into_renderer().render(ctx);
                    acc.push(rendered_field);
                    (acc, ctx)
                });

        let indented: Vec<String> = rendered_fields
            .into_iter()
            .map(|f| format!("{}{}", ctx.indent(), f))
            .collect();

        let rendered = format!("type {} {{\n{}\n}}", input_object.name, indented.join("\n"));

        ctx.add(input_object.name.clone(), rendered.clone());
        (rendered, ctx)
    }

    fn render_output_object(&self, output_object: &ObjectTypeRef, ctx: RenderContext) -> (String, RenderContext) {
        if ctx.already_rendered(&output_object.name) {
            return ("".into(), ctx);
        } else {
            // This short circuits recursive processing for fields.
            ctx.mark_as_rendered(output_object.name.clone())
        }

        let (rendered_fields, ctx): (Vec<String>, RenderContext) =
            output_object
                .get_fields()
                .iter()
                .fold((vec![], ctx), |(mut acc, ctx), field| {
                    let (rendered_field, ctx) = field.into_renderer().render(ctx);
                    acc.push(rendered_field);
                    (acc, ctx)
                });

        let indented: Vec<String> = rendered_fields
            .into_iter()
            .map(|f| format!("{}{}", ctx.indent(), f))
            .collect();

        let rendered = format!("type {} {{\n{}\n}}", output_object.name, indented.join("\n"));

        ctx.add_output(rendered.clone());
        (rendered, ctx)
    }
}
