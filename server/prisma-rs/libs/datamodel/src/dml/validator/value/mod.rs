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
}

impl ValueParserError {
    pub fn wrap<T, E: error::Error>(result: Result<T, E>, raw_value: &String) -> Result<T, ValueParserError> {
        match result {
            Ok(val) => Ok(val),
            Err(err) => Err(ValueParserError::new(err.description().to_string(), raw_value.clone())),
        }
    }

    pub fn new(message: String, raw: String) -> ValueParserError {
        ValueParserError {
            message: message,
            raw: raw,
        }
    }
}

impl fmt::Display for ValueParserError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", self.message)
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
    ($value:expr, $wrapper:expr, $raw:expr) => ({
        match $value {
            Ok(val) => Ok($wrapper(val)),
            Err(err) => Err(ValueParserError::new(err.description().to_string(), $raw))
        }
    })
);

pub trait ValueValidator {
    fn is_valid(&self) -> bool;

    fn raw(&self) -> &String;
    fn as_str(&self) -> Result<String, ValueParserError>;
    fn as_int(&self) -> Result<i32, ValueParserError>;
    fn as_float(&self) -> Result<f32, ValueParserError>;
    fn as_decimal(&self) -> Result<f32, ValueParserError>;
    fn as_bool(&self) -> Result<bool, ValueParserError>;
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError>;
    fn as_constant_literal(&self) -> Result<String, ValueParserError>;

    fn as_type(&self, scalar_type: &dml::ScalarType) -> Result<dml::Value, ValueParserError> {
        match scalar_type {
            dml::ScalarType::Int => wrap_value!(self.as_int(), dml::Value::Int, self.raw().clone()),
            dml::ScalarType::Float => wrap_value!(self.as_float(), dml::Value::Float, self.raw().clone()),
            dml::ScalarType::Decimal => wrap_value!(self.as_decimal(), dml::Value::Decimal, self.raw().clone()),
            dml::ScalarType::Boolean => wrap_value!(self.as_bool(), dml::Value::Boolean, self.raw().clone()),
            dml::ScalarType::DateTime => wrap_value!(self.as_date_time(), dml::Value::DateTime, self.raw().clone()),
            dml::ScalarType::Enum => wrap_value!(self.as_str(), dml::Value::ConstantLiteral, self.raw().clone()),
            dml::ScalarType::String => wrap_value!(self.as_str(), dml::Value::String, self.raw().clone()),
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
            ast::Value::StringValue(x) => x,
            ast::Value::NumericValue(x) => x,
            ast::Value::BooleanValue(x) => x,
            ast::Value::ConstantValue(x) => x,
        }
    }

    fn as_str(&self) -> Result<String, ValueParserError> {
        match &self.value {
            ast::Value::StringValue(value) => Ok(value.to_string()),
            _ => Err(ValueParserError::new(
                format!("Expected String Value, received {:?}", self.value),
                self.raw().clone(),
            )),
        }
    }

    fn as_int(&self) -> Result<i32, ValueParserError> {
        match &self.value {
            ast::Value::NumericValue(value) => ValueParserError::wrap(value.parse::<i32>(), value),
            _ => Err(ValueParserError::new(
                format!("Expected Numeric Value, received {:?}", self.value),
                self.raw().clone(),
            )),
        }
    }

    fn as_float(&self) -> Result<f32, ValueParserError> {
        match &self.value {
            ast::Value::NumericValue(value) => ValueParserError::wrap(value.parse::<f32>(), value),
            _ => Err(ValueParserError::new(
                format!("Expected Numeric Value, received {:?}", self.value),
                self.raw().clone(),
            )),
        }
    }

    // TODO: Ask which decimal type to take.
    fn as_decimal(&self) -> Result<f32, ValueParserError> {
        match &self.value {
            ast::Value::NumericValue(value) => ValueParserError::wrap(value.parse::<f32>(), value),
            _ => Err(ValueParserError::new(
                format!("Expected Numeric Value, received {:?}", self.value),
                self.raw().clone(),
            )),
        }
    }

    fn as_bool(&self) -> Result<bool, ValueParserError> {
        match &self.value {
            ast::Value::BooleanValue(value) => ValueParserError::wrap(value.parse::<bool>(), value),
            _ => Err(ValueParserError::new(
                format!("Expected Boolean Value, received {:?}", self.value),
                self.raw().clone(),
            )),
        }
    }

    // TODO: Ask which datetime type to use.
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError> {
        match &self.value {
            ast::Value::StringValue(value) => ValueParserError::wrap(value.parse::<DateTime<Utc>>(), value),
            _ => Err(ValueParserError::new(
                format!("Expected Boolean Value, received {:?}", self.value),
                self.raw().clone(),
            )),
        }
    }

    fn as_constant_literal(&self) -> Result<String, ValueParserError> {
        match &self.value {
            ast::Value::ConstantValue(value) => Ok(value.to_string()),
            _ => Err(ValueParserError::new(
                format!("Expected Constant Value, received {:?}", self.value),
                self.raw().clone(),
            )),
        }
    }
}

pub struct WrappedErrorValue {
    // TODO: Make everything str&
    pub message: String,
    pub raw: String,
}

impl ValueValidator for WrappedErrorValue {
    fn is_valid(&self) -> bool {
        false
    }

    fn raw(&self) -> &String {
        &self.raw
    }

    fn as_str(&self) -> Result<String, ValueParserError> {
        Err(ValueParserError::new(self.message.clone(), self.raw.clone()))
    }
    fn as_int(&self) -> Result<i32, ValueParserError> {
        Err(ValueParserError::new(self.message.clone(), self.raw.clone()))
    }
    fn as_float(&self) -> Result<f32, ValueParserError> {
        Err(ValueParserError::new(self.message.clone(), self.raw.clone()))
    }
    fn as_decimal(&self) -> Result<f32, ValueParserError> {
        Err(ValueParserError::new(self.message.clone(), self.raw.clone()))
    }
    fn as_bool(&self) -> Result<bool, ValueParserError> {
        Err(ValueParserError::new(self.message.clone(), self.raw.clone()))
    }
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError> {
        Err(ValueParserError::new(self.message.clone(), self.raw.clone()))
    }
    fn as_constant_literal(&self) -> Result<String, ValueParserError> {
        Err(ValueParserError::new(self.message.clone(), self.raw.clone()))
    }
}
