use super::ErrorWithSpan;
use crate::ast::Span;

#[derive(Debug, Clone)]
pub struct ArgumentNotFoundError {
    pub argument_name: String,
    pub directive_name: String,
    pub span: Span,
}

impl ArgumentNotFoundError {
    pub fn new(argument_name: &str, directive_name: &str, span: &Span) -> ArgumentNotFoundError {
        ArgumentNotFoundError {
            argument_name: String::from(argument_name),
            directive_name: String::from(directive_name),
            span: span.clone(),
        }
    }
}

impl ErrorWithSpan for ArgumentNotFoundError {
    fn span(&self) -> Span {
        self.span
    }
}

impl std::fmt::Display for ArgumentNotFoundError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(
            f,
            "Argument {} is missing in directive @{}",
            self.argument_name, self.directive_name
        )
    }
}
