use super::ErrorWithSpan;
use crate::ast::Span;

#[derive(Debug)]
pub struct ParserError {
    pub message: String,
    pub span: Span,
}

impl ParserError {
    pub fn new(message: &str, span: &Span) -> ParserError {
        ParserError {
            message: String::from(message),
            span: span.clone(),
        }
    }
}

impl ErrorWithSpan for ParserError {
    fn span(&self) -> Span {
        self.span
    }
}

impl std::fmt::Display for ParserError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{}", self.message)
    }
}
