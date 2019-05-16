use crate::errors::LiteralParseError;
use crate::ast::Span;

pub trait FromStrAndSpan: Sized {
    fn from_str_and_span(s: &str, span: &Span) -> Result<Self, LiteralParseError>;
}