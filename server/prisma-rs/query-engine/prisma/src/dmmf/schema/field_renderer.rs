use super::*;

#[derive(Debug)]
pub enum DMMFFieldRenderer<'a> {
    Input(&'a InputField),
    Output(FieldRef),
}

impl<'a> Renderer<'a, DMMFFieldWrapper> for DMMFFieldRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> (DMMFFieldWrapper, RenderContext) {
        match &self {
            DMMFFieldRenderer::Input(input) => self.render_input_field(input, ctx),
            DMMFFieldRenderer::Output(output) => self.render_output_field(Arc::clone(output), ctx),
        }
    }
}

impl<'a> DMMFFieldRenderer<'a> {
    fn render_input_field(&self, input_field: &InputField, ctx: RenderContext) -> (DMMFFieldWrapper, RenderContext) {
        let (type_info, ctx) = input_field.field_type.into_renderer().render(ctx);
        let field = DMMFInputField {
            name: input_field.name.clone(),
            input_type: type_info,
        };

        (DMMFFieldWrapper::Input(field), ctx)
    }

    fn render_output_field(&self, field: FieldRef, ctx: RenderContext) -> (DMMFFieldWrapper, RenderContext) {
        let (args, ctx) = self.render_arguments(&field.arguments, ctx);
        let (output_type, ctx) = field.field_type.into_renderer().render(ctx);
        let output_field = DMMFField {
            name: field.name.clone(),
            args,
            output_type,
        };

        ctx.add_mapping(field.name.clone(), field.operation.as_ref());
        (DMMFFieldWrapper::Output(output_field), ctx)
    }

    fn render_arguments(&self, args: &Vec<Argument>, ctx: RenderContext) -> (Vec<DMMFArgument>, RenderContext) {
        args.iter().fold((vec![], ctx), |(mut prev, ctx), arg| {
            let (rendered, ctx) = self.render_argument(arg, ctx);

            prev.push(rendered);
            (prev, ctx)
        })
    }

    fn render_argument(&self, arg: &Argument, ctx: RenderContext) -> (DMMFArgument, RenderContext) {
        let (input_type, ctx) = (&arg.argument_type).into_renderer().render(ctx);
        let rendered_arg = DMMFArgument {
            name: arg.name.clone(),
            input_type,
        };

        (rendered_arg, ctx)
    }
}
