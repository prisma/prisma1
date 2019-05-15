use super::{LiteralParseError, ErrorWithSpan};
use crate::ast::Span;

#[derive(Debug, Clone)]
pub struct ValueParserError {
    expected_type: Option<String>,
    received_type: Option<String>,
    format_parser_error: Option<String>,
    raw: String,
    span: Span,
}

// TODO: This class handles too many cases. It's super ugly.
impl ValueParserError {
    pub fn new_for_format_error(expected_type: &str, error_message: &str, raw: &str, span: &Span) -> ValueParserError {
        ValueParserError {
            expected_type: Some(String::from(expected_type)),
            received_type: None,
            format_parser_error: Some(String::from(error_message)),
            raw: String::from(raw),
            span: span.clone(),
        }
    }

    pub fn new(expected_type: &str, received_type: &str, raw: &str, span: &Span) -> ValueParserError {
        ValueParserError {
            expected_type: Some(String::from(expected_type)),
            received_type: Some(String::from(received_type)),
            format_parser_error: None,
            raw: String::from(raw),
            span: span.clone(),
        }
    }

    pub fn new_from_literal_parser_error(error: &LiteralParseError) -> ValueParserError {
        ValueParserError {
            expected_type: None,
            received_type: None,
            format_parser_error: Some(format!("{}", error)),
            raw: error.raw_value.clone(),
            span: error.span.clone()
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
        match (&self.expected_type, &self.received_type, &self.format_parser_error) {
            (Some(expected_type), Some(received_type), None) => write!(
                f,
                "Expected {}, but received {} value {}.",
                expected_type, received_type, self.raw
            ),
            (Some(expected_type), None, Some(parser_error)) => write!(
                f,
                "Expected {}, but failed while parsing {}: {}",
                expected_type, self.raw, parser_error
            ),

            (None, None, Some(parser_error)) => write!(
                f,
                "{}", parser_error
            ),
            _ => unimplemented!("Impossible error."),
        }
    }
}
