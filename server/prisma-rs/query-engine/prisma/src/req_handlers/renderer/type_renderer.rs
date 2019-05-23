use super::*;

pub enum GqlTypeRenderer<'a> {
    Input(&'a InputType),
    Output(&'a OutputType),
}

impl<'a> Renderer for GqlTypeRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> RenderContext {
        // if self.rendered.contains_key(&typ.name) {
        //     return;
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

        // self.rendered.insert(typ.name.clone(), ()).unwrap();
        // self.output_queue.push(formatted);

        unimplemented!()
    }

    // fn render_fields<T>(&self, fields: &Vec<T>) -> Vec<String>
    // where
    //     T: IntoRenderer,
    // {
    //     fields.into_iter().map(|f| self.render_field::<T>(f)).collect()
    // }
}
