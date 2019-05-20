use crate::ast;
use crate::common::value;
use crate::errors::ValidationError;

pub struct Arguments<'a> {
    arguments: &'a Vec<ast::Argument>,
    span: ast::Span,
}

impl<'a> Arguments<'a> {
    pub fn new(arguments: &'a Vec<ast::Argument>, span: ast::Span) -> Arguments<'a> {
        Arguments {
            arguments: arguments,
            span: span.clone(),
        }
    }

    pub fn span(&self) -> &ast::Span {
        &self.span
    }

    pub fn arg(&self, name: &str) -> Result<value::ValueValidator, ValidationError> {
        for arg in self.arguments {
            if arg.name == name {
                return Ok(value::ValueValidator {
                    value: arg.value.clone(),
                });
            }
        }
        return Err(ValidationError::new_argument_not_found_error(name, &self.span));
    }

    pub fn default_arg(&self, name: &str) -> Result<value::ValueValidator, ValidationError> {
        let arg = self.arg(name);

        match arg {
            Ok(arg) => Ok(arg),
            // TODO: This will probably lead to a misleading err message.
            Err(_) => self.arg(""),
        }
    }
}
