use crate::ast::parser::{parse_expression, PrismaDatamodelParser, Rule};
use crate::ast::{Span, Value};
use crate::common::value::ValueValidator;
use crate::errors::ValidationError;
use pest::Parser;

pub struct StringInterpolator {}

fn lift_span(span: &Span, offset: usize) -> Span {
    Span {
        start: offset + span.start,
        end: offset + span.end,
    }
}

/// Parses an expression and adds an offset to the span start, so we have consistent error
/// messages.
fn parse_expr_and_lift_span(token: &pest::iterators::Pair<'_, Rule>, start: usize) -> Value {
    match parse_expression(token) {
        Value::NumericValue(v, s) => Value::NumericValue(v, lift_span(&s, start)),
        Value::BooleanValue(v, s) => Value::BooleanValue(v, lift_span(&s, start)),
        Value::StringValue(v, s) => Value::StringValue(v, lift_span(&s, start)),
        Value::ConstantValue(v, s) => Value::ConstantValue(v, lift_span(&s, start)),
        Value::Function(n, a, s) => Value::Function(n, a, lift_span(&s, start)),
    }
}

impl StringInterpolator {
    pub fn interpolate(text: &str, span: &Span) -> Result<Value, ValidationError> {
        let string_result = PrismaDatamodelParser::parse(Rule::string_interpolated, text);
        let mut parts: Vec<String> = Vec::new();

        match string_result {
            Ok(mut string_wrapped) => {
                let string_components = string_wrapped.next().unwrap();

                match_children! { string_components, current,
                    Rule::string_uninterpolated => parts.push(String::from(current.as_str())),
                    Rule::expression => {
                        let value = parse_expr_and_lift_span(&current, span.start);
                        parts.push(ValueValidator::new(&value)?.raw().clone())
                    },
                    Rule::EOI => {},
                    _ => panic!("Encounterd impossible datamodel declaration during parsing: {:?}", current.tokens())
                }

                Ok(Value::StringValue(parts.join(""), span.clone()))
            }
            Err(err) => match err.location {
                pest::error::InputLocation::Pos(pos) => Err(ValidationError::new_parser_error(
                    "Unexpected token while interpolating string.",
                    &Span::new(pos + span.start, pos + span.start),
                )),
                pest::error::InputLocation::Span((from, to)) => Err(ValidationError::new_parser_error(
                    "Unexpected token while interpolating string.",
                    &Span::new(from + span.start, to + span.start),
                )),
            },
        }
    }
}
