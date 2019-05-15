use crate::ast;
use crate::dml::validator::value;

pub struct DirectiveArguments<'a> {
    arguments: &'a Vec<ast::DirectiveArgument>,
    span: ast::Span
}

impl<'a> DirectiveArguments<'a> {
    pub fn new(arguments: &'a Vec<ast::DirectiveArgument>, span: ast::Span) -> DirectiveArguments {
        DirectiveArguments { arguments: arguments, span: span.clone() }
    }

    pub fn span(&self) -> &ast::Span {
        &self.span
    }

    pub fn arg(&self, name: &str) -> Box<value::ValueValidator> {
        for arg in self.arguments {
            if arg.name == name {
                return Box::new(value::WrappedValue {
                    value: arg.value.clone()
                });
            }
        }
        return Box::new(value::WrappedErrorValue {
            message: format!("Argument '{:?}' not found", name),
            raw: String::from(""),
            span: self.span
        });
    }

    pub fn default_arg(&self, name: &str) -> Box<value::ValueValidator> {
        let arg = self.arg(name);

        if arg.is_valid() {
            return arg;
        } else {
            // Fallback to default arg without name.
            return self.arg("");
        }
    }
}
