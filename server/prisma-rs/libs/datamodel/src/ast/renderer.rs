use super::string_builder::StringBuilder;
use super::table::TableFormat;
use crate::ast;

pub trait LineWriteable {
    fn write(&mut self, param: &str);
    fn line_empty(&self) -> bool;
    fn end_line(&mut self);
    fn maybe_end_line(&mut self);
}

pub struct Renderer<'a> {
    stream: &'a mut std::io::Write,
    indent: usize,
    new_line: usize,
    is_new: bool,
    maybe_new_line: usize,
    indent_width: usize,
}

// TODO: It would be soooo cool if we could pass format strings around.
impl<'a> Renderer<'a> {
    pub fn new(stream: &'a mut std::io::Write, indent_width: usize) -> Renderer<'a> {
        Renderer {
            stream,
            indent: 0,
            indent_width,
            new_line: 0,
            maybe_new_line: 0,
            is_new: true,
        }
    }

    pub fn render(&mut self, datamodel: &ast::Datamodel) {
        let mut type_renderer: Option<TableFormat> = None;

        for (i, top) in datamodel.models.iter().enumerate() {
            match &top {
                // TODO: This is super ugly. Goal is that type groups get
                // formatted togehter.
                ast::Top::Type(custom_type) => {
                    if type_renderer.is_none() {
                        if i != 0 {
                            // We put an extra line break in between top level structs.
                            self.end_line();
                        }
                        type_renderer = Some(TableFormat::new());
                    }
                    if let Some(renderer) = &mut type_renderer {
                        Self::render_custom_type(renderer, custom_type);
                    }
                }
                other => {
                    if let Some(renderer) = &type_renderer {
                        renderer.render(self);
                        type_renderer = None;
                    }

                    if i != 0 {
                        // We put an extra line break in between top level structs.
                        self.end_line();
                    }

                    match other {
                        ast::Top::Model(model) => self.render_model(model),
                        ast::Top::Enum(enm) => self.render_enum(enm),
                        ast::Top::Source(source) => self.render_source_block(source),
                        ast::Top::Generator(generator) => self.render_generator_block(generator),
                        ast::Top::Type(_) => unreachable!(),
                    }
                }
            };
        }
    }

    pub fn render_documentation(target: &mut LineWriteable, obj: &ast::WithDocumentation) {
        if let Some(doc) = &obj.documentation() {
            for line in doc.text.split("\n") {
                target.write("/// ");
                target.write(line);
                target.end_line();
            }
        }
    }

    pub fn render_source_block(&mut self, source: &ast::SourceConfig) {
        Self::render_documentation(self, source);

        self.write("datasource ");
        self.write(&source.name);
        self.write(" {");
        self.end_line();
        self.indent_up();

        let mut formatter = TableFormat::new();

        for property in &source.properties {
            formatter.write(&property.name);
            formatter.write(" = ");
            formatter.write(&Self::render_value_to_string(&property.value));
            formatter.end_line();
        }

        formatter.render(self);

        self.indent_down();
        self.write("}");
        self.end_line();
    }

    pub fn render_generator_block(&mut self, generator: &ast::GeneratorConfig) {
        Self::render_documentation(self, generator);

        self.write("generator ");
        self.write(&generator.name);
        self.write(" {");
        self.end_line();
        self.indent_up();

        let mut formatter = TableFormat::new();

        for property in &generator.properties {
            formatter.write(&property.name);
            formatter.write(" = ");
            formatter.write(&Self::render_value_to_string(&property.value));
            formatter.end_line();
        }

        formatter.render(self);

        self.indent_down();
        self.write("}");
        self.end_line();
    }

    pub fn render_custom_type(target: &mut TableFormat, field: &ast::Field) {
        Self::render_documentation(&mut target.interleave_writer(), field);

        target.write("type ");
        target.write(&field.name);
        target.write(&" = ");
        target.write(&field.field_type);

        // Attributes
        if field.directives.len() > 0 {
            let mut attributes_builder = StringBuilder::new();

            for directive in &field.directives {
                Self::render_field_directive(&mut attributes_builder, &directive);
            }

            target.write(&attributes_builder.to_string());
        }

        target.end_line();
    }

    pub fn render_model(&mut self, model: &ast::Model) {
        Self::render_documentation(self, model);

        self.write("model ");
        self.write(&model.name);
        self.write(" {");
        self.end_line();
        self.indent_up();

        let mut field_formatter = TableFormat::new();

        for field in &model.fields {
            Self::render_field(&mut field_formatter, &field);
        }

        field_formatter.render(self);

        if model.directives.len() > 0 {
            self.end_line();
            for directive in &model.directives {
                self.render_block_directive(&directive);
            }
        }

        self.indent_down();
        self.write("}");
        self.end_line();
    }

    pub fn render_enum(&mut self, enm: &ast::Enum) {
        Self::render_documentation(self, enm);

        self.write("enum ");
        self.write(&enm.name);
        self.write(" {");
        self.end_line();
        self.indent_up();

        for value in &enm.values {
            self.write(&value);
            self.end_line();
        }

        if enm.directives.len() > 0 {
            self.end_line();
            for directive in &enm.directives {
                self.write(" ");
                self.render_block_directive(&directive);
            }
        }

        self.indent_down();
        self.write("}");
        self.end_line();
    }

    pub fn render_field(target: &mut TableFormat, field: &ast::Field) {
        Self::render_documentation(&mut target.interleave_writer(), field);

        target.write(&field.name);

        // Type
        {
            let mut type_builder = StringBuilder::new();

            type_builder.write(&field.field_type);
            Self::render_field_arity(&mut type_builder, &field.arity);

            target.write(&type_builder.to_string());
        }

        // Attributes
        if field.directives.len() > 0 {
            let mut attributes_builder = StringBuilder::new();

            for directive in &field.directives {
                attributes_builder.write(&" ");
                Self::render_field_directive(&mut attributes_builder, &directive);
            }

            target.write(&attributes_builder.to_string());
        }

        target.end_line();
    }

    pub fn render_field_arity(target: &mut LineWriteable, field_arity: &ast::FieldArity) {
        match field_arity {
            ast::FieldArity::List => target.write("[]"),
            ast::FieldArity::Optional => target.write("?"),
            ast::FieldArity::Required => {}
        };
    }

    pub fn render_field_directive(target: &mut LineWriteable, directive: &ast::Directive) {
        target.write("@");
        target.write(&directive.name);

        if directive.arguments.len() > 0 {
            target.write("(");
            Self::render_arguments(target, &directive.arguments);
            target.write(")");
        }
    }

    pub fn render_block_directive(&mut self, directive: &ast::Directive) {
        self.write("@@");
        self.write(&directive.name);

        if directive.arguments.len() > 0 {
            self.write("(");
            Self::render_arguments(self, &directive.arguments);
            self.write(")");
        }
        self.end_line();
    }

    pub fn render_arguments(target: &mut LineWriteable, args: &Vec<ast::Argument>) {
        for (idx, arg) in args.iter().enumerate() {
            if idx > 0 {
                target.write(&", ");
            }
            Self::render_argument(target, arg);
        }
    }

    pub fn render_argument(target: &mut LineWriteable, args: &ast::Argument) {
        if args.name != "" {
            target.write(&args.name);
            target.write(&": ");
        }

        Self::render_value(target, &args.value);
    }

    pub fn render_value_to_string(val: &ast::Value) -> String {
        let mut builder = StringBuilder::new();
        Self::render_value(&mut builder, val);
        builder.to_string()
    }

    pub fn render_value(target: &mut LineWriteable, val: &ast::Value) {
        match val {
            ast::Value::Array(vals, _) => Self::render_array(target, &vals),
            ast::Value::BooleanValue(val, _) => target.write(&val),
            ast::Value::ConstantValue(val, _) => target.write(&val),
            ast::Value::NumericValue(val, _) => target.write(&val),
            ast::Value::StringValue(val, _) => Self::render_str(target, &val),
            ast::Value::Function(name, args, _) => Self::render_func(target, &name, &args),
            ast::Value::Any(_, _) => unimplemented!("Value of 'Any' type cannot be rendered."),
        };
    }

    pub fn render_func(target: &mut LineWriteable, name: &str, vals: &Vec<ast::Value>) {
        target.write(name);
        target.write("(");
        for val in vals {
            Self::render_value(target, val);
        }
        target.write(")");
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

    pub fn render_array(target: &mut LineWriteable, vals: &Vec<ast::Value>) {
        target.write(&"[");
        for (idx, arg) in vals.iter().enumerate() {
            if idx > 0 {
                target.write(&", ");
            }
            Self::render_value(target, arg);
        }
        target.write(&"]");
    }

    fn render_str(target: &mut LineWriteable, param: &str) {
        target.write("\"");
        target.write(param);
        target.write("\"");
    }
}

impl<'a> LineWriteable for Renderer<'a> {
    fn write(&mut self, param: &str) {
        self.is_new = false;
        // TODO: Proper result handling.
        if self.new_line > 0 || self.maybe_new_line > 0 {
            for _i in 0..std::cmp::max(self.new_line, self.maybe_new_line) {
                writeln!(self.stream, "").expect("Writer error.");
            }
            write!(self.stream, "{}", " ".repeat(self.indent * self.indent_width)).expect("Writer error.");
            self.new_line = 0;
            self.maybe_new_line = 0;
        }

        write!(self.stream, "{}", param).expect("Writer error.");
    }

    fn end_line(&mut self) {
        self.new_line = self.new_line + 1;
    }

    fn maybe_end_line(&mut self) {
        self.maybe_new_line = self.maybe_new_line + 1;
    }

    fn line_empty(&self) -> bool {
        self.new_line != 0 || self.maybe_new_line != 0 || self.is_new
    }
}
