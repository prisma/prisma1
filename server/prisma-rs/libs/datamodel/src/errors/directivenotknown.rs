use super::ErrorWithSpan;
use crate::ast::Span;

#[derive(Debug, Clone)]
pub struct DirectiveNotKnownError {
    pub directive_name: String,
    pub span: Span,
}

impl DirectiveNotKnownError {
    pub fn new(directive_name: &str, span: &Span) -> DirectiveNotKnownError {
        DirectiveNotKnownError {
            directive_name: String::from(directive_name),
            span: span.clone(),
        }
    }
}

impl ErrorWithSpan for DirectiveNotKnownError {
    fn span(&self) -> Span {
        self.span
    }
}

impl std::fmt::Display for DirectiveNotKnownError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "Directive not known: @{}", self.directive_name)
    }
}