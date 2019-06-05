use crate::ast;

pub struct Renderer<'a> {
    stream: &'a mut std::io::Write,
    indent: usize,
}

// TODO: It would be soooo cool if we could pass format strings around.

/// Default indent width.
const INDENT_WIDTH: usize = 4;

impl<'a> Renderer<'a> {
    pub fn new(stream: &'a mut std::io::Write) -> Renderer<'a> {
        Renderer { stream, indent: 0 }
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
        self.write("model ");
        self.write(&model.name);
        self.write(" {");
        self.indent_up();

        for field in &model.fields {
            self.render_field(&field);
        }

        for directive in &model.directives {
            self.render_block_directive(&directive);
        }

        self.indent_down();
        self.begin_line();
        self.write("}");
        self.begin_line();
    }

    pub fn render_enum(&mut self, enm: &ast::Enum) {
        self.begin_line();
        self.write("enum ");
        self.write(&enm.name);
        self.write(" {");
        self.indent_up();

        for value in &enm.values {
            self.begin_line();
            self.write(&value);
        }

        for directive in &enm.directives {
            self.write(" ");
            self.render_block_directive(&directive);
        }

        self.indent_down();
        self.begin_line();
        self.write("}");
        self.begin_line();
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
            ast::FieldArity::List => self.write("[]"),
            ast::FieldArity::Optional => self.write("?"),
            ast::FieldArity::Required => {}
        };
    }

    pub fn render_field_directive(&mut self, directive: &ast::Directive) {
        self.write("@");
        self.write(&directive.name);

        if directive.arguments.len() > 0 {
            self.write("(");
            self.render_arguments(&directive.arguments);
            self.write(")");
        }
    }

    pub fn render_block_directive(&mut self, directive: &ast::Directive) {
        self.begin_line();
        self.write("@@");
        self.write(&directive.name);

        if directive.arguments.len() > 0 {
            self.write("(");
            self.render_arguments(&directive.arguments);
            self.write(")");
        }
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
            ast::Value::Function(name, args, _) => self.render_func(&name, &args),
        };
    }

    pub fn render_func(&mut self, name: &str, vals: &Vec<ast::Value>) {
        self.write(name);
        self.write("(");
        for val in vals {
            self.render_value(val);
        }
        self.write(")");
    }

    pub fn indent_up(&mut self) {
        self.indent = self.indent + 1
    }

    pub fn indent_down(&mut self) {
        if self.indent == 0 {
            panic!("Indentation error.")
        }
        self.indent = self.indent - 1
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

    fn render_str(&mut self, param: &str) {
        self.write("\"");
        self.write(param);
        self.write("\"");
    }

    fn write(&mut self, param: &str) {
        // TODO: Proper result handling.
        write!(self.stream, "{}", param).expect("Writer error.");
    }

    fn begin_line(&mut self) {
        writeln!(self.stream, "").expect("Writer error.");
        write!(self.stream, "{}", " ".repeat(self.indent * INDENT_WIDTH)).expect("Writer error.");
    }
}
