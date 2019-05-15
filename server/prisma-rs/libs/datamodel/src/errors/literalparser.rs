use super::ErrorWithSpan;
use crate::ast::Span;

#[derive(Debug, Clone)]
pub struct LiteralParseError {
    pub literal_type: String,
    pub raw_value: String,
    pub span: Span,
}

impl LiteralParseError {
    pub fn new(literal_type: &str, raw_value: &str, span: &Span) -> LiteralParseError {
        LiteralParseError {
            literal_type: String::from(literal_type),
            raw_value: String::from(raw_value),
            span: span.clone(),
        }
    }
}

impl ErrorWithSpan for LiteralParseError {
    fn span(&self) -> Span {
        self.span
    }
}

impl std::fmt::Display for LiteralParseError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{} is not a valid value for {}", self.raw_value, self.literal_type)
    }
}
