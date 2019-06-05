use crate::ast::parser::{parse_expression, PrismaDatamodelParser, Rule};
use crate::ast::{Span, Value};
use crate::common::value::ValueValidator;
use crate::errors::ValidationError;
use pest::Parser;

pub struct StringInterpolator {}

/// Adds an offset to a span.
fn lift_span(span: &Span, offset: usize) -> Span {
    Span {
        start: offset + span.start,
        end: offset + span.end,
    }
}

/// Parses an expression and adds an offset to the span start, so we have consistent error
/// messages.
fn parse_expr_and_lift_span(token: &pest::iterators::Pair<'_, Rule>, start: usize) -> Result<Value, ValidationError> {
    match parse_expression(token) {
        Value::NumericValue(v, s) => Ok(Value::NumericValue(v, lift_span(&s, start))),
        Value::BooleanValue(v, s) => Ok(Value::BooleanValue(v, lift_span(&s, start))),
        Value::StringValue(v, s) => Ok(Value::StringValue(v, lift_span(&s, start))),
        Value::ConstantValue(v, s) => Ok(Value::ConstantValue(v, lift_span(&s, start))),
        Value::Function(n, a, s) => Ok(Value::Function(n, a, lift_span(&s, start))),
        Value::Array(_, s) => Err(ValidationError::new_validation_error(
            "Arrays cannot be interpolated into strings.",
            &s,
        )),
        Value::ServerSideFunction(_, _, _, s) => Err(ValidationError::new_validation_error(
            "Functions cannot be interpolated into strings.",
            &s,
        )),
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

                match_children! { string_components, current,
                    // Explicit handling of escaped `${`, like `\${...}`.
                    Rule::string_escaped_interpolation => parts.push(String::from("${")),
                    Rule::string_any => parts.push(String::from(current.as_str())),
                    Rule::expression => {
                        let value = parse_expr_and_lift_span(&current, span.start)?;
                        parts.push(String::from(ValueValidator::new(&value)?.raw()))
                    },
                    Rule::EOI => {},
                    _ => panic!("Encounterd impossible interpolated string during parsing: {:?}", current.tokens())
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
