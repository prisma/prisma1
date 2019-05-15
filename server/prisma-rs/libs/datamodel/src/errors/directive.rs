use super::{ArgumentNotFoundError, ErrorWithSpan};
use crate::ast::Span;

#[derive(Debug)]
pub struct DirectiveValidationError {
    pub message: String,
    pub directive_name: String,
    pub span: Span,
}

impl DirectiveValidationError {
    pub fn new(message: &str, directive_name: &str, span: &Span) -> DirectiveValidationError {
        DirectiveValidationError {
            message: String::from(message),
            directive_name: String::from(directive_name),
            span: span.clone(),
        }
    }
}

impl ErrorWithSpan for DirectiveValidationError {
    fn span(&self) -> Span {
        self.span
    }
}

impl std::fmt::Display for DirectiveValidationError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "Error parsing directive @{}: {}", self.directive_name, self.message)
    }
}

impl std::convert::From<ArgumentNotFoundError> for DirectiveValidationError {
    fn from(error: ArgumentNotFoundError) -> Self {
        DirectiveValidationError::new(&format!("{}", error), &error.directive_name, &error.span)
    }
}
