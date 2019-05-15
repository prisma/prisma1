use super::ErrorWithSpan;
use crate::ast::Span;

#[derive(Debug, Clone)]
pub struct ValueParserError {
    expected_type: String,
    received_type: Option<String>,
    format_parser_error: Option<String>,
    raw: String,
    span: Span,
}

impl ValueParserError {
    pub fn new_for_format_error(expected_type: &str, error_message: &str, raw: &str, span: &Span) -> ValueParserError {
        ValueParserError {
            expected_type: String::from(expected_type),
            received_type: None,
            format_parser_error: Some(String::from(error_message)),
            raw: String::from(raw),
            span: span.clone(),
        }
    }

    pub fn new(expected_type: &str, received_type: &str, raw: &str, span: &Span) -> ValueParserError {
        ValueParserError {
            expected_type: String::from(expected_type),
            received_type: Some(String::from(received_type)),
            format_parser_error: None,
            raw: String::from(raw),
            span: span.clone(),
        }
    }
}

impl ErrorWithSpan for ValueParserError {
    fn span(&self) -> Span {
        self.span
    }
}

impl std::fmt::Display for ValueParserError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        match (&self.received_type, &self.format_parser_error) {
            (Some(received_type), None) => write!(
                f,
                "Expected {}, but received {} value {}.",
                self.expected_type, received_type, self.raw
            ),
            (None, Some(parser_error)) => write!(
                f,
                "Expected {}, but failed while parsing {}: {}",
                self.expected_type, self.raw, parser_error
            ),
            _ => unimplemented!("Impossible error."),
        }
    }
}
