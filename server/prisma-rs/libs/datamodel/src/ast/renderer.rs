use crate::ast;

pub struct Renderer<'a> {
    stream: &'a mut std::io::Write,
}

// TODO: It would be soooo cool if we could pass format strings around.

impl<'a> Renderer<'a> {
    pub fn new(stream: &'a mut std::io::Write) -> Renderer<'a> {
        Renderer { stream }
    }

    pub fn render(&mut self, datamodel: &ast::Datamodel) {
        for top in &datamodel.models {
            match top {
                ast::Top::Model(model) => self.render_model(model),
                ast::Top::Enum(enm) => self.render_enum(enm),
                ast::Top::Source(_) => unimplemented!("Source block rendering is not implemented."),
            };
        }
    }

    pub fn render_model(&mut self, model: &ast::Model) {
        self.begin_line();
        self.write(&"model ");
        self.write(&model.name);
        self.write(&" {");

        for field in &model.fields {
            self.render_field(&field);
        }

        for directive in &model.directives {
            self.render_block_directive(&directive);
        }

        self.begin_line();
        self.write(&"}");
    }

    pub fn render_enum(&mut self, enm: &ast::Enum) {
        self.begin_line();
        self.write(&"enum ");
        self.write(&enm.name);
        self.write(&" {");

        for value in &enm.values {
            self.begin_line();
            self.write(&value);
        }

        for directive in &enm.directives {
            self.write(&" ");
            self.render_block_directive(&directive);
        }

        self.begin_line();
        self.write(&"}");
    }

    pub fn render_field(&mut self, field: &ast::Field) {
        self.begin_line();
        self.write(&field.name);
        self.write(&" ");
        self.write(&field.field_type);
        self.render_field_arity(&field.arity);

        for directive in &field.directives {
            self.write(&" ");
            self.render_field_directive(&directive);
        }
    }

    pub fn render_field_arity(&mut self, field_arity: &ast::FieldArity) {
        match field_arity {
            ast::FieldArity::List => self.write(&"[]"),
            ast::FieldArity::Optional => self.write(&"?"),
            ast::FieldArity::Required => {}
        };
    }

    pub fn render_field_directive(&mut self, directive: &ast::Directive) {
        self.write(&"@");
        self.write(&directive.name);
        self.write(&"(");
        self.render_arguments(&directive.arguments);
        self.write(&")");
    }

    pub fn render_block_directive(&mut self, directive: &ast::Directive) {
        self.begin_line();
        self.write(&"@@");
        self.write(&directive.name);
        self.write(&"(");
        self.render_arguments(&directive.arguments);
        self.write(&")");
    }

    pub fn render_arguments(&mut self, args: &Vec<ast::Argument>) {
        for (idx, arg) in args.iter().enumerate() {
            if idx > 0 {
                self.write(&", ");
            }
            self.render_argument(arg);
        }
    }

    pub fn render_argument(&mut self, args: &ast::Argument) {
        if args.name != "" {
            self.write(&args.name);
            self.write(&": ");
        }

        self.render_value(&args.value);
    }

    pub fn render_value(&mut self, val: &ast::Value) {
        match val {
            ast::Value::Array(vals, _) => self.render_array(&vals),
            ast::Value::BooleanValue(val, _) => self.write(&val),
            ast::Value::ConstantValue(val, _) => self.write(&val),
            ast::Value::NumericValue(val, _) => self.write(&val),
            ast::Value::StringValue(val, _) => self.render_str(&val),
            ast::Value::Function(_, _, _) => unimplemented!("Functions can not be rendered yet."),
        };
    }

    pub fn render_array(&mut self, vals: &Vec<ast::Value>) {
        self.write(&"[");
        for (idx, arg) in vals.iter().enumerate() {
            if idx > 0 {
                self.write(&", ");
            }
            self.render_value(arg);
        }
        self.write(&"]");
    }

    fn render_str(&mut self, param: &std::fmt::Display) {
        self.write(&"\"");
        self.write(param);
        self.write(&"\"");
    }

    fn write(&mut self, param: &std::fmt::Display) {
        // TODO: Proper result handling.
        write!(self.stream, "{}", param).expect("Writer error.");
    }

    fn begin_line(&mut self) {
        writeln!(self.stream, "").expect("Writer error.");
    }
}
