use super::*;

pub enum GqlFieldRenderer<'a> {
    Input(&'a InputField),
    Output(&'a Field),
}

impl<'a> Renderer for GqlFieldRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> RenderContext {
        match &self {
            GqlFieldRenderer::Input(input) => unimplemented!(),
            GqlFieldRenderer::Output(output) => {
                let rendered_args = self.render_arguments(&output.arguments);
            }
        };

        unimplemented!()
    }
}

impl<'a> GqlFieldRenderer<'a> {
    fn render_input_field() -> String {
        unimplemented!()
    }

    fn render_output_field() -> String {
        unimplemented!()
    }

    fn render_arguments(&self, args: &Vec<Argument>) -> String {
        unimplemented!()
    }
}
