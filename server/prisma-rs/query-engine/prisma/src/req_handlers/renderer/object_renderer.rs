use super::*;

pub enum GqlObjectRenderer {
    Input(InputObjectTypeRef),
    Output(ObjectTypeRef),
}

impl Renderer for GqlObjectRenderer {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext) {
        match &self {
            GqlObjectRenderer::Input(input) => unimplemented!(),
            GqlObjectRenderer::Output(output) => unimplemented!(),
        }
    }
}

impl GqlObjectRenderer {
    // fn render_fields<T>(&self, fields: &Vec<T>) -> Vec<String>
    // where
    //     T: IntoRenderer,
    // {
    //     fields.into_iter().map(|f| self.render_field::<T>(f)).collect()
    // }

    // let rendered_fields = self.render_fields(&typ.fields.get().unwrap());
    // let indented: Vec<String> = rendered_fields
    //     .into_iter()
    //     .map(|s| format!("{}{}", " ".repeat(self.indent), s))
    //     .collect();

    // let formatted = format!(
    //     "type {} {{
    //         {}
    //     }}",
    //     typ.name,
    //     indented.join("\n")
    // );

    // if self.rendered.contains_key(&typ.name) {
    //     return;
    // }

    // self.rendered.insert(typ.name.clone(), ()).unwrap();
    // self.output_queue.push(formatted);
}
