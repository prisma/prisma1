use super::*;

pub struct GqlEnumRenderer {
    enum_type_ref: EnumTypeRef,
}

impl Renderer for GqlEnumRenderer {
    fn render(&self, ctx: RenderContext) -> (String, RenderContext) {
        if ctx.already_rendered(&self.enum_type_ref.name) {
            return ("".into(), ctx);
        }

        let values: Vec<String> = self
            .enum_type_ref
            .values
            .iter()
            .map(|v| format!("{}{}", ctx.indent(), self.format_enum_value(v)))
            .collect();

        let rendered = format!("enum {} {{\n{}\n}}", self.enum_type_ref.name, values.join("\n"));

        ctx.add(self.enum_type_ref.name.clone(), rendered.clone());
        (rendered, ctx)
    }
}

impl GqlEnumRenderer {
    pub fn new(enum_type_ref: EnumTypeRef) -> GqlEnumRenderer {
        GqlEnumRenderer { enum_type_ref }
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
