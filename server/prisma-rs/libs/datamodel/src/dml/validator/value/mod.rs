use crate::ast;
use crate::dml;

use crate::errors::ValidationError;
use chrono::{DateTime, Utc};
use std::error;

macro_rules! wrap_value (
    ($value:expr, $wrapper:expr, $validator:expr) => ({
        match $value {
            Ok(val) => Ok($wrapper(val)),
            Err(err) => Err(err)
        }
    })
);

pub struct ValueValidator {
    pub value: ast::Value,
}

impl ValueValidator {
    fn construct_error(&self, expected_type: &str) -> ValidationError {
        ValidationError::new_type_mismatch_error(
            expected_type,
            ast::describe_value_type(&self.value),
            self.raw(),
            self.span(),
        )
    }

    fn wrap_error_from_result<T, E: error::Error>(
        &self,
        result: Result<T, E>,
        expected_type: &str,
    ) -> Result<T, ValidationError> {
        match result {
            Ok(val) => Ok(val),
            Err(err) => Err(ValidationError::new_value_parser_error(
                expected_type,
                err.description(),
                self.raw(),
                self.span(),
            )),
        }
    }

    pub fn value(&self) -> &ast::Value {
        &self.value
    }

    pub fn as_type(&self, scalar_type: &dml::ScalarType) -> Result<dml::Value, ValidationError> {
        match scalar_type {
            dml::ScalarType::Int => wrap_value!(self.as_int(), dml::Value::Int, self),
            dml::ScalarType::Float => wrap_value!(self.as_float(), dml::Value::Float, self),
            dml::ScalarType::Decimal => wrap_value!(self.as_decimal(), dml::Value::Decimal, self),
            dml::ScalarType::Boolean => wrap_value!(self.as_bool(), dml::Value::Boolean, self),
            dml::ScalarType::DateTime => wrap_value!(self.as_date_time(), dml::Value::DateTime, self),
            dml::ScalarType::Enum => wrap_value!(self.as_str(), dml::Value::ConstantLiteral, self),
            dml::ScalarType::String => wrap_value!(self.as_str(), dml::Value::String, self),
        }
    }

    pub fn parse_literal<T: dml::FromStrAndSpan>(&self) -> Result<T, ValidationError> {
        T::from_str_and_span(&self.as_constant_literal()?, self.span())
    }

    pub fn raw(&self) -> &String {
        match &self.value {
            ast::Value::StringValue(x, _) => x,
            ast::Value::NumericValue(x, _) => x,
            ast::Value::BooleanValue(x, _) => x,
            ast::Value::ConstantValue(x, _) => x,
        }
    }

    pub fn span(&self) -> &ast::Span {
        match &self.value {
            ast::Value::StringValue(_, s) => s,
            ast::Value::NumericValue(_, s) => s,
            ast::Value::BooleanValue(_, s) => s,
            ast::Value::ConstantValue(_, s) => s,
        }
    }

    pub fn as_str(&self) -> Result<String, ValidationError> {
        match &self.value {
            ast::Value::StringValue(value, _) => Ok(value.to_string()),
            _ => Err(self.construct_error("String")),
        }
    }

    pub fn as_int(&self) -> Result<i32, ValidationError> {
        match &self.value {
            ast::Value::NumericValue(value, _) => self.wrap_error_from_result(value.parse::<i32>(), "Numeric"),
            _ => Err(self.construct_error("Numeric")),
        }
    }

    pub fn as_float(&self) -> Result<f32, ValidationError> {
        match &self.value {
            ast::Value::NumericValue(value, _) => self.wrap_error_from_result(value.parse::<f32>(), "Numeric"),
            _ => Err(self.construct_error("Numeric")),
        }
    }

    // TODO: Ask which decimal type to take.
    pub fn as_decimal(&self) -> Result<f32, ValidationError> {
        match &self.value {
            ast::Value::NumericValue(value, _) => self.wrap_error_from_result(value.parse::<f32>(), "Numeric"),
            _ => Err(self.construct_error("Numeric")),
        }
    }

    pub fn as_bool(&self) -> Result<bool, ValidationError> {
        match &self.value {
            ast::Value::BooleanValue(value, _) => self.wrap_error_from_result(value.parse::<bool>(), "Boolean"),
            _ => Err(self.construct_error("Boolean")),
        }
    }

    // TODO: Ask which datetime type to use.
    pub fn as_date_time(&self) -> Result<DateTime<Utc>, ValidationError> {
        match &self.value {
            ast::Value::StringValue(value, _) => {
                self.wrap_error_from_result(value.parse::<DateTime<Utc>>(), "String-Like")
            }
            _ => Err(self.construct_error("String-Like")),
        }
    }

    pub fn as_constant_literal(&self) -> Result<String, ValidationError> {
        match &self.value {
            ast::Value::ConstantValue(value, _) => Ok(value.to_string()),
            _ => Err(self.construct_error("Constant Literal")),
        }
    }
}
