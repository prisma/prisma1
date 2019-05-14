use crate::ast;
use crate::dml;

use chrono::{DateTime, Utc};
use std::error;
use std::error::Error;
use std::fmt;

#[derive(Debug)]
pub struct ValueParserError {
    pub message: String,
    pub raw: String,
    pub span: ast::Span,
}

impl ValueParserError {
    pub fn wrap<T, E: error::Error>(
        result: Result<T, E>,
        raw_value: &str,
        span: &ast::Span,
    ) -> Result<T, ValueParserError> {
        match result {
            Ok(val) => Ok(val),
            Err(err) => Err(ValueParserError::new(err.description(), raw_value, span)),
        }
    }

    pub fn new(message: &str, raw: &str, span: &ast::Span) -> ValueParserError {
        ValueParserError {
            message: String::from(message),
            raw: String::from(raw),
            span: span.clone(),
        }
    }
}

impl fmt::Display for ValueParserError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}, Value: {}, Location: {}", self.message, self.raw, self.span)
    }
}

impl error::Error for ValueParserError {
    fn description(&self) -> &str {
        self.message.as_str()
    }

    fn cause(&self) -> Option<&error::Error> {
        None
    }
}

macro_rules! wrap_value (
    ($value:expr, $wrapper:expr, $raw:expr, $span:expr) => ({
        match $value {
            Ok(val) => Ok($wrapper(val)),
            Err(err) => Err(ValueParserError::new(err.description(), $raw, $span))
        }
    })
);

pub trait ValueValidator {
    fn is_valid(&self) -> bool;

    fn raw(&self) -> &String;
    fn span(&self) -> &ast::Span;
    fn as_str(&self) -> Result<String, ValueParserError>;
    fn as_int(&self) -> Result<i32, ValueParserError>;
    fn as_float(&self) -> Result<f32, ValueParserError>;
    fn as_decimal(&self) -> Result<f32, ValueParserError>;
    fn as_bool(&self) -> Result<bool, ValueParserError>;
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError>;
    fn as_constant_literal(&self) -> Result<String, ValueParserError>;

    fn as_type(&self, scalar_type: &dml::ScalarType) -> Result<dml::Value, ValueParserError> {
        match scalar_type {
            dml::ScalarType::Int => wrap_value!(self.as_int(), dml::Value::Int, self.raw(), self.span()),
            dml::ScalarType::Float => wrap_value!(self.as_float(), dml::Value::Float, self.raw(), self.span()),
            dml::ScalarType::Decimal => wrap_value!(self.as_decimal(), dml::Value::Decimal, self.raw(), self.span()),
            dml::ScalarType::Boolean => wrap_value!(self.as_bool(), dml::Value::Boolean, self.raw(), self.span()),
            dml::ScalarType::DateTime => {
                wrap_value!(self.as_date_time(), dml::Value::DateTime, self.raw(), self.span())
            }
            dml::ScalarType::Enum => wrap_value!(self.as_str(), dml::Value::ConstantLiteral, self.raw(), self.span()),
            dml::ScalarType::String => wrap_value!(self.as_str(), dml::Value::String, self.raw(), self.span()),
        }
    }
}

// TODO: Inject error accumulation.
// TODO: Inject location (line etc.) information into error type.
pub struct WrappedValue {
    pub value: ast::Value,
}

impl ValueValidator for WrappedValue {
    fn is_valid(&self) -> bool {
        true
    }

    fn raw(&self) -> &String {
        match &self.value {
            ast::Value::StringValue(x, _) => x,
            ast::Value::NumericValue(x, _) => x,
            ast::Value::BooleanValue(x, _) => x,
            ast::Value::ConstantValue(x, _) => x,
        }
    }

    fn span(&self) -> &ast::Span {
        match &self.value {
            ast::Value::StringValue(_, s) => s,
            ast::Value::NumericValue(_, s) => s,
            ast::Value::BooleanValue(_, s) => s,
            ast::Value::ConstantValue(_, s) => s,
        }
    }

    fn as_str(&self) -> Result<String, ValueParserError> {
        match &self.value {
            ast::Value::StringValue(value, _) => Ok(value.to_string()),
            _ => Err(ValueParserError::new(
                &format!("Expected String Value, received {:?}", self.value),
                self.raw(),
                self.span(),
            )),
        }
    }

    fn as_int(&self) -> Result<i32, ValueParserError> {
        match &self.value {
            ast::Value::NumericValue(value, span) => ValueParserError::wrap(value.parse::<i32>(), value, span),
            _ => Err(ValueParserError::new(
                &format!("Expected Numeric Value, received {:?}", self.value),
                self.raw(),
                self.span(),
            )),
        }
    }

    fn as_float(&self) -> Result<f32, ValueParserError> {
        match &self.value {
            ast::Value::NumericValue(value, span) => ValueParserError::wrap(value.parse::<f32>(), value, span),
            _ => Err(ValueParserError::new(
                &format!("Expected Numeric Value, received {:?}", self.value),
                self.raw(),
                self.span(),
            )),
        }
    }

    // TODO: Ask which decimal type to take.
    fn as_decimal(&self) -> Result<f32, ValueParserError> {
        match &self.value {
            ast::Value::NumericValue(value, span) => ValueParserError::wrap(value.parse::<f32>(), value, span),
            _ => Err(ValueParserError::new(
                &format!("Expected Numeric Value, received {:?}", self.value),
                self.raw(),
                self.span(),
            )),
        }
    }

    fn as_bool(&self) -> Result<bool, ValueParserError> {
        match &self.value {
            ast::Value::BooleanValue(value, span) => ValueParserError::wrap(value.parse::<bool>(), value, span),
            _ => Err(ValueParserError::new(
                &format!("Expected Boolean Value, received {:?}", self.value),
                self.raw(),
                self.span(),
            )),
        }
    }

    // TODO: Ask which datetime type to use.
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError> {
        match &self.value {
            ast::Value::StringValue(value, span) => ValueParserError::wrap(value.parse::<DateTime<Utc>>(), value, span),
            _ => Err(ValueParserError::new(
                &format!("Expected Boolean Value, received {:?}", self.value),
                self.raw(),
                self.span(),
            )),
        }
    }

    fn as_constant_literal(&self) -> Result<String, ValueParserError> {
        match &self.value {
            ast::Value::ConstantValue(value, _) => Ok(value.to_string()),
            _ => Err(ValueParserError::new(
                &format!("Expected Constant Value, received {:?}", self.value),
                self.raw(),
                self.span(),
            )),
        }
    }
}

pub struct WrappedErrorValue {
    pub message: String,
    pub raw: String,
    pub span: ast::Span,
}

impl ValueValidator for WrappedErrorValue {
    fn is_valid(&self) -> bool {
        false
    }

    fn raw(&self) -> &String {
        &self.raw
    }

    fn span(&self) -> &ast::Span {
        &self.span
    }

    fn as_str(&self) -> Result<String, ValueParserError> {
        Err(ValueParserError::new(&self.message, &self.raw, &self.span))
    }
    fn as_int(&self) -> Result<i32, ValueParserError> {
        Err(ValueParserError::new(&self.message, &self.raw, &self.span))
    }
    fn as_float(&self) -> Result<f32, ValueParserError> {
        Err(ValueParserError::new(&self.message, &self.raw, &self.span))
    }
    fn as_decimal(&self) -> Result<f32, ValueParserError> {
        Err(ValueParserError::new(&self.message, &self.raw, &self.span))
    }
    fn as_bool(&self) -> Result<bool, ValueParserError> {
        Err(ValueParserError::new(&self.message, &self.raw, &self.span))
    }
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError> {
        Err(ValueParserError::new(&self.message, &self.raw, &self.span))
    }
    fn as_constant_literal(&self) -> Result<String, ValueParserError> {
        Err(ValueParserError::new(&self.message, &self.raw, &self.span))
    }
}
