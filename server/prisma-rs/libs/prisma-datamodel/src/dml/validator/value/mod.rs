use crate::ast;
use crate::dml;

use chrono::{Utc, DateTime};
use std::fmt;
use std::error;
use std::error::Error;

#[derive(Debug)]
pub struct ValueParserError {
    pub message: String
}

impl ValueParserError {
    pub fn wrap<T, E: error::Error>(result: Result<T, E>) -> Result<T, ValueParserError> {
        match result {
            Ok(val) => Ok(val),
            Err(err) => Err(ValueParserError::new(err.description().to_string()))
        }
    }

    pub fn new(message: String) -> ValueParserError {
        ValueParserError { message: message }
    }
}

impl fmt::Display for ValueParserError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
       write!(f, "{}",self.message)
    }
}

impl error::Error for ValueParserError {
    fn description(&self) -> &str {
        "Parser error"
    }

    fn cause(&self) -> Option<&error::Error> {
        None
    }
}

macro_rules! wrap_value (
    ($value:expr, $wrapper:expr) => ({
        match $value {
            Ok(val) => Ok($wrapper(val)),
            Err(err) => Err(ValueParserError::new(err.description().to_string()))
        }
    })
);

pub trait ValueValidator {
    fn as_str(&self) -> Result<String, ValueParserError>;
    fn as_int(&self) -> Result<i32, ValueParserError>;
    fn as_float(&self) -> Result<f32, ValueParserError>;
    fn as_decimal(&self) -> Result<f32, ValueParserError>;
    fn as_bool(&self) -> Result<bool, ValueParserError>;
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError>;
    fn as_constant_literal(&self) -> Result<String, ValueParserError>;

    fn as_type(&self, scalar_type: &dml::ScalarType) -> Result<dml::Value, ValueParserError> {
        match scalar_type {
            dml::ScalarType::Int => wrap_value!(self.as_int(), dml::Value::Int),
            dml::ScalarType::Float => wrap_value!(self.as_float(), dml::Value::Float),
            dml::ScalarType::Decimal => wrap_value!(self.as_decimal(), dml::Value::Decimal),
            dml::ScalarType::Boolean => wrap_value!(self.as_bool(), dml::Value::Boolean),
            dml::ScalarType::DateTime => wrap_value!(self.as_date_time(), dml::Value::DateTime),
            dml::ScalarType::Enum => wrap_value!(self.as_str(), dml::Value::ConstantLiteral),
            dml::ScalarType::String => wrap_value!(self.as_str(), dml::Value::String)
        }
    }
}

// TODO: Inject error accumulation.
// TODO: Inject location (line etc.) information into error type.
pub struct WrappedValue {
    pub value: ast::Value
}

impl ValueValidator for WrappedValue {
    fn as_str(&self) -> Result<String, ValueParserError> {
        match &self.value {
            ast::Value::StringValue(value) => Ok(value.to_string()),
            _ => Err(ValueParserError::new(format!("Expected String Value, received {:?}", self.value)))
        }
    }
    
    fn as_int(&self) -> Result<i32, ValueParserError>{
        match &self.value {
            ast::Value::NumericValue(value) => ValueParserError::wrap(value.parse::<i32>()),
            _ => Err(ValueParserError::new(format!("Expected Numeric Value, received {:?}", self.value)))
        }
    }

    fn as_float(&self) -> Result<f32, ValueParserError> {
        match &self.value {
            ast::Value::NumericValue(value) => ValueParserError::wrap(value.parse::<f32>()),
            _ => Err(ValueParserError::new(format!("Expected Numeric Value, received {:?}", self.value)))
        }
    }

    // TODO: Ask which decimal type to take.
    fn as_decimal(&self) -> Result<f32, ValueParserError> {
        match &self.value {
            ast::Value::NumericValue(value) => ValueParserError::wrap(value.parse::<f32>()),
            _ => Err(ValueParserError::new(format!("Expected Numeric Value, received {:?}", self.value)))
        }
    }


    fn as_bool(&self) -> Result<bool, ValueParserError> {
        match &self.value {
            ast::Value::BooleanValue(value) => ValueParserError::wrap(value.parse::<bool>()),
            _ => Err(ValueParserError::new(format!("Expected Boolean Value, received {:?}", self.value)))
        }
    }

    // TODO: Ask which datetime type to use.
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError>{
        match &self.value {
            ast::Value::StringValue(value) => ValueParserError::wrap(value.parse::<DateTime<Utc>>()),
            _ => Err(ValueParserError::new(format!("Expected Boolean Value, received {:?}", self.value)))
        }
    }

    fn as_constant_literal(&self) -> Result<String, ValueParserError> {
        match &self.value {
            ast::Value::ConstantValue(value) => Ok(value.to_string()),
            _ => Err(ValueParserError::new(format!("Expected Constant Value, received {:?}", self.value)))
        }
    }
}

pub struct WrappedErrorValue {
    // TODO: Make everything str&
    pub message: String
}

impl ValueValidator for WrappedErrorValue {
    fn as_str(&self) -> Result<String, ValueParserError> { Err(ValueParserError::new(self.message.clone())) }
    fn as_int(&self) -> Result<i32, ValueParserError> { Err(ValueParserError::new(self.message.clone())) }
    fn as_float(&self) -> Result<f32, ValueParserError> { Err(ValueParserError::new(self.message.clone())) }
    fn as_decimal(&self) -> Result<f32, ValueParserError> { Err(ValueParserError::new(self.message.clone())) }
    fn as_bool(&self) -> Result<bool, ValueParserError> { Err(ValueParserError::new(self.message.clone())) }
    fn as_date_time(&self) -> Result<DateTime<Utc>, ValueParserError> { Err(ValueParserError::new(self.message.clone())) }
    fn as_constant_literal(&self) -> Result<String, ValueParserError> { Err(ValueParserError::new(self.message.clone())) }
}