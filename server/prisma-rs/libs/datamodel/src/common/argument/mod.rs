use crate::ast;
use crate::common::value;
use crate::errors::ValidationError;

/// Represents a list of arguments.
///
/// This class makes it more convenient to implement
/// custom directives.
pub struct Arguments<'a> {
    arguments: &'a Vec<ast::Argument>,
    span: ast::Span,
}

impl<'a> Arguments<'a> {
    /// Creates a new instance, given a list of arguments.
    pub fn new(arguments: &'a Vec<ast::Argument>, span: ast::Span) -> Arguments<'a> {
        Arguments {
            arguments: arguments,
            span: span.clone(),
        }
    }

    /// Creates empty arguments. The vec is a dummy that needs to be handed in
    /// due of the shape of the struct.
    pub fn empty(vec: &'a Vec<ast::Argument>) -> Self {
        Arguments {
            arguments: vec,
            span: ast::Span::empty(),
        }
    }

    /// Gets the span of all arguments wrapped by this instance.
    pub fn span(&self) -> &ast::Span {
        &self.span
    }

    /// Gets the arg with the given name.
    pub fn arg(&self, name: &str) -> Result<value::ValueValidator, ValidationError> {
        for arg in self.arguments {
            if arg.name == name {
                return value::ValueValidator::new(&arg.value);
            }
        }
        return Err(ValidationError::new_argument_not_found_error(name, &self.span));
    }

    /// Gets the arg with the given name, or if it is not found, the first unnamed argument.
    ///
    /// Use this to implement unnamed argument behavior.
    pub fn default_arg(&self, name: &str) -> Result<value::ValueValidator, ValidationError> {
        let arg = self.arg(name);

        match arg {
            Ok(arg) => Ok(arg),
            // TODO: This will probably lead to a misleading err message.
            Err(_) => self.arg(""),
        }
    }
}
