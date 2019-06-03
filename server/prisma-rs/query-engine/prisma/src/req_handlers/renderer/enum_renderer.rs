use super::*;

pub struct GqlEnumRenderer<'a> {
    enum_type: &'a EnumType,
}

impl<'a> Renderer for GqlEnumRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext) {
        if ctx.already_rendered(&self.enum_type.name) {
            return ("".into(), ctx);
        }

        let values: Vec<String> = self
            .enum_type
            .values
            .iter()
            .map(|v| format!("{}{}", ctx.indent(), self.format_enum_value(v)))
            .collect();

        let rendered = format!("enum {} {{\n{}\n}}", self.enum_type.name, values.join("\n"));

        ctx.add(self.enum_type.name.clone(), rendered.clone());
        (rendered, ctx)
    }
}

impl<'a> GqlEnumRenderer<'a> {
    pub fn new(enum_type: &EnumType) -> GqlEnumRenderer {
        GqlEnumRenderer { enum_type }
    }

    fn format_enum_value(&self, value: &EnumValue) -> String {
        self.format_enum_value_wrapper(&value.value)
    }

    fn format_enum_value_wrapper(&self, value: &EnumValueWrapper) -> String {
        match value {
            EnumValueWrapper::OrderBy(order_by) => {
                format!("{}_{}", order_by.field.name, order_by.sort_order.abbreviated())
            }
            EnumValueWrapper::String(s) => s.clone(),
        }
    }
}
