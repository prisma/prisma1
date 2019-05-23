use super::*;

pub struct GqlEnumRenderer {
    enum_type_ref: EnumTypeRef,
}

impl Renderer for GqlEnumRenderer {
    fn render(&self, ctx: RenderContext) -> RenderContext {
        unimplemented!()
    }
}

impl GqlEnumRenderer {
    pub fn new(enum_type_ref: EnumTypeRef) -> GqlEnumRenderer {
        GqlEnumRenderer { enum_type_ref }
    }
}
