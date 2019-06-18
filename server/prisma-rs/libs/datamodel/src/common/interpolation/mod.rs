use crate::ast::parser::{parse_expression, PrismaDatamodelParser, Rule};
use crate::ast::{lift_span, Span, Value};
use crate::common::value::ValueValidator;
use crate::errors::ValidationError;
use pest::Parser;

pub struct StringInterpolator {}

/// Parses an expression and adds an offset to the span start, so we have consistent error
/// messages.
fn parse_expr_and_lift_span(token: &pest::iterators::Pair<'_, Rule>, start: usize) -> Result<Value, ValidationError> {
    match parse_expression(token) {
        Value::Array(_, s) => Err(ValidationError::new_validation_error(
            "Arrays cannot be interpolated into strings.",
            &lift_span(&s, start),
        )),
        expr => Ok(expr.with_lifted_span(start)),
    }
}

/// Struct which helps with interpolating strings.
impl StringInterpolator {
    /// Interpolates expressions inside strings.alloc
    ///
    /// The string is re-parsed and all expressions found within `${...}` are
    /// evaluated recursively.
    pub fn interpolate(text: &str, span: &Span) -> Result<Value, ValidationError> {
        let string_result = PrismaDatamodelParser::parse(Rule::string_interpolated, text);
        let mut parts: Vec<String> = Vec::new();

        match string_result {
            Ok(mut string_wrapped) => {
                let string_components = string_wrapped.next().unwrap();

                for current in string_components.into_inner() {
                    match current.as_rule() {
                        Rule::string_interpolate_escape => {
                            for child in current.into_inner() {
                                match child.as_rule() {
                                    Rule::WHITESPACE => {}
                                    Rule::COMMENT => {}
                                    Rule::INTERPOLATION_START => {}
                                    Rule::INTERPOLATION_END => {}
                                    Rule::expression => {
                                        let value = parse_expr_and_lift_span(&child, span.start + 1)?;
                                        parts.push(String::from(ValueValidator::new(&value)?.raw()))
                                    }
                                    Rule::EOI => {}
                                    _ => panic!("Encounterd impossible interpolation sequence: {:?}", child.tokens()),
                                };
                            }
                        }
                        // Explicit handling of escaped `${`, like `\${...}`.
                        Rule::string_escaped_interpolation => parts.push(String::from("${")),
                        Rule::string_any => parts.push(String::from(current.as_str())),
                        Rule::expression => {
                            let value = parse_expr_and_lift_span(&current, span.start + 1)?;
                            parts.push(String::from(ValueValidator::new(&value)?.raw()))
                        }
                        // No whitespace, no comments.
                        Rule::EOI => {}
                        _ => panic!(
                            "Encounterd impossible interpolated string during parsing: {:?}",
                            current.tokens()
                        ),
                    };
                }

                Ok(Value::StringValue(parts.join(""), span.clone()))
            }
            Err(err) => {
                let location = match err.location {
                    pest::error::InputLocation::Pos(pos) => Span::new(pos, pos),
                    pest::error::InputLocation::Span((from, to)) => Span::new(from, to),
                };

                let expected = match err.variant {
                    pest::error::ErrorVariant::ParsingError {
                        positives,
                        negatives: _,
                    } => crate::ast::parser::get_expected_from_error(&positives),
                    _ => panic!("Could not construct parsing error. This should never happend."),
                };

                Err(ValidationError::new_parser_error(&expected, &location))
            }
        }
    }
}
