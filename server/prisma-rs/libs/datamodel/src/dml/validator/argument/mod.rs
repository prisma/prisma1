use crate::ast;
use crate::dml::validator::value;
use crate::errors::ArgumentNotFoundError;

pub struct DirectiveArguments<'a> {
    directive_name: String,
    arguments: &'a Vec<ast::DirectiveArgument>,
    span: ast::Span,
}

impl<'a> DirectiveArguments<'a> {
    pub fn new(
        arguments: &'a Vec<ast::DirectiveArgument>,
        directive_name: &str,
        span: ast::Span,
    ) -> DirectiveArguments<'a> {
        DirectiveArguments {
            directive_name: String::from(directive_name),
            arguments: arguments,
            span: span.clone(),
        }
    }

    pub fn span(&self) -> &ast::Span {
        &self.span
    }

    pub fn arg(&self, name: &str) -> Result<value::ValueValidator, ArgumentNotFoundError> {
        for arg in self.arguments {
            if arg.name == name {
                return Ok(value::ValueValidator {
                    value: arg.value.clone(),
                });
            }
        }
        return Err(ArgumentNotFoundError::new(name, &self.directive_name, &self.span));
    }

    pub fn default_arg(&self, name: &str) -> Result<value::ValueValidator, ArgumentNotFoundError> {
        let arg = self.arg(name);

        match arg {
            Ok(arg) => Ok(arg),
            // TODO: This will probably lead to a misleading err message.
            Err(_) => self.arg(""),
        }
    }
}
