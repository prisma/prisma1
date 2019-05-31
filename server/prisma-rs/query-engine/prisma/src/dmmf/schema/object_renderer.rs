use super::*;

#[derive(Debug)]
pub enum DMMFObjectRenderer {
    Input(InputObjectTypeRef),
    Output(ObjectTypeRef),
}

impl<'a> Renderer<'a, ()> for DMMFObjectRenderer {
    fn render(&self, ctx: RenderContext) -> ((), RenderContext) {
        match &self {
            DMMFObjectRenderer::Input(input) => self.render_input_object(input, ctx),
            DMMFObjectRenderer::Output(output) => self.render_output_object(output, ctx),
        }
    }
}

impl DMMFObjectRenderer {
    fn render_input_object(&self, input_object: &InputObjectTypeRef, ctx: RenderContext) -> ((), RenderContext) {
        let input_object = input_object.into_arc();
        if ctx.already_rendered(&input_object.name) {
            return ((), ctx);
        } else {
            // This short circuits recursive processing for fields.
            ctx.mark_as_rendered(input_object.name.clone())
        }

        let (rendered_fields, ctx): (Vec<DMMFInputField>, RenderContext) =
            input_object
                .get_fields()
                .iter()
                .fold((vec![], ctx), |(mut acc, ctx), field| {
                    let (rendered_field, ctx) = field.into_renderer().render(ctx);
                    match rendered_field {
                        DMMFFieldWrapper::Input(f) => acc.push(f),
                        _ => unreachable!(),
                    };

                    (acc, ctx)
                });

        let input_type = DMMFInputType {
            name: input_object.name.clone(),
            fields: rendered_fields,
        };

        ctx.add_input_type(input_type);
        ((), ctx)
    }

    // WIP dedup code
    fn render_output_object(&self, output_object: &ObjectTypeRef, ctx: RenderContext) -> ((), RenderContext) {
        let output_object = output_object.into_arc();
        if ctx.already_rendered(&output_object.name) {
            return ((), ctx);
        } else {
            // This short circuits recursive processing for fields.
            ctx.mark_as_rendered(output_object.name.clone())
        }

        let (rendered_fields, ctx): (Vec<DMMFField>, RenderContext) =
            output_object
                .get_fields()
                .iter()
                .fold((vec![], ctx), |(mut acc, ctx), field| {
                    let (rendered_field, ctx) = field.into_renderer().render(ctx);
                    match rendered_field {
                        DMMFFieldWrapper::Output(f) => acc.push(f),
                        _ => unreachable!(),
                    };

                    (acc, ctx)
                });

        let output_type = DMMFOutputType {
            name: output_object.name.clone(),
            fields: rendered_fields,
        };

        ctx.add_output_type(output_type);
        ((), ctx)
    }
}
