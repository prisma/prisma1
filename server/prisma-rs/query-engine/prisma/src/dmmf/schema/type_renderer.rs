use super::*;

#[derive(Debug)]
pub enum DMMFTypeRenderer<'a> {
    Input(&'a InputType),
    Output(&'a OutputType),
}

impl<'a> Renderer<'a, DMMFTypeInfo> for DMMFTypeRenderer<'a> {
    fn render(&self, ctx: RenderContext) -> (DMMFTypeInfo, RenderContext) {
        match self {
            DMMFTypeRenderer::Input(i) => self.render_input_type(i, ctx),
            DMMFTypeRenderer::Output(o) => self.render_output_type(o, ctx),
        }
    }
}

impl<'a> DMMFTypeRenderer<'a> {
    fn render_input_type(&self, i: &InputType, ctx: RenderContext) -> (DMMFTypeInfo, RenderContext) {
        match i {
            InputType::Object(ref obj) => {
                let (_, subctx) = obj.into_renderer().render(ctx);
                let type_info = DMMFTypeInfo {
                    typ: obj.into_arc().name.clone(),
                    kind: TypeKind::Object,
                    is_required: true,
                    is_list: false,
                };

                (type_info, subctx)
            }
            InputType::Enum(et) => {
                let (_, subctx) = et.into_renderer().render(ctx);
                let type_info = DMMFTypeInfo {
                    typ: et.name.clone(),
                    kind: TypeKind::Enum,
                    is_required: true,
                    is_list: false,
                };

                (type_info, subctx)
            }
            InputType::List(ref l) => {
                let (mut type_info, subctx) = self.render_input_type(l, ctx);
                type_info.is_list = true;

                (type_info, subctx)
            }
            InputType::Opt(ref opt) => {
                let (mut type_info, subctx) = self.render_input_type(opt, ctx);
                type_info.is_required = false;

                (type_info, subctx)
            }
            InputType::Scalar(ScalarType::Enum(et)) => {
                let (_, subctx) = et.into_renderer().render(ctx);
                let type_info = DMMFTypeInfo {
                    typ: et.name.clone(),
                    kind: TypeKind::Scalar,
                    is_required: true,
                    is_list: false,
                };

                (type_info, subctx)
            }
            InputType::Scalar(ref scalar) => {
                let stringified = match scalar {
                    ScalarType::String => "String",
                    ScalarType::Int => "Int",
                    ScalarType::Boolean => "Boolean",
                    ScalarType::Float => "Float",
                    ScalarType::DateTime => "DateTime",
                    ScalarType::Json => "DateTime",
                    ScalarType::ID => "ID",
                    ScalarType::UUID => "UUID",
                    ScalarType::Enum(_) => unreachable!(), // Handled separately above.
                };

                let type_info = DMMFTypeInfo {
                    typ: stringified.into(),
                    kind: TypeKind::Scalar,
                    is_required: true,
                    is_list: false,
                };

                (type_info, ctx)
            }
        }
    }

    // WIP dedup code
    fn render_output_type(&self, o: &OutputType, ctx: RenderContext) -> (DMMFTypeInfo, RenderContext) {
        match o {
            OutputType::Object(ref obj) => {
                let (_, subctx) = obj.into_renderer().render(ctx);
                let type_info = DMMFTypeInfo {
                    typ: obj.into_arc().name.clone(),
                    kind: TypeKind::Object,
                    is_required: true,
                    is_list: false,
                };

                (type_info, subctx)
            }
            OutputType::Enum(et) => {
                let (_, subctx) = et.into_renderer().render(ctx);
                let type_info = DMMFTypeInfo {
                    typ: et.name.clone(),
                    kind: TypeKind::Enum,
                    is_required: true,
                    is_list: false,
                };

                (type_info, subctx)
            }
            OutputType::List(ref l) => {
                let (mut type_info, subctx) = self.render_output_type(l, ctx);
                type_info.is_list = true;

                (type_info, subctx)
            }
            OutputType::Opt(ref opt) => {
                let (mut type_info, subctx) = self.render_output_type(opt, ctx);
                type_info.is_required = false;

                (type_info, subctx)
            }
            OutputType::Scalar(ScalarType::Enum(et)) => {
                let (_, subctx) = et.into_renderer().render(ctx);
                let type_info = DMMFTypeInfo {
                    typ: et.name.clone(),
                    kind: TypeKind::Scalar,
                    is_required: true,
                    is_list: false,
                };

                (type_info, subctx)
            }
            OutputType::Scalar(ref scalar) => {
                let stringified = match scalar {
                    ScalarType::String => "String",
                    ScalarType::Int => "Int",
                    ScalarType::Boolean => "Boolean",
                    ScalarType::Float => "Float",
                    ScalarType::DateTime => "DateTime",
                    ScalarType::Json => "DateTime",
                    ScalarType::ID => "ID",
                    ScalarType::UUID => "UUID",
                    ScalarType::Enum(_) => unreachable!(), // Handled separately above.
                };

                let type_info = DMMFTypeInfo {
                    typ: stringified.into(),
                    kind: TypeKind::Scalar,
                    is_required: true,
                    is_list: false,
                };

                (type_info, ctx)
            }
        }
    }
}
