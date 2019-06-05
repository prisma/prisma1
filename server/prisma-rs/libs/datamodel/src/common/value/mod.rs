use crate::ast;
use crate::dml;

use crate::common::interpolation::StringInterpolator;
use crate::errors::ValidationError;
use crate::FunctionalEvaluator;
use chrono::{DateTime, Utc};
use std::error;
use crate::common::PrismaType;
use crate::common::FromStrAndSpan;

macro_rules! wrap_value (
    ($value:expr, $wrapper:expr, $validator:expr) => ({
        match $value {
            Ok(val) => Ok($wrapper(val)),
            Err(err) => Err(err)
        }
    })
);

/// Wraps a value and provides convenience methods for
/// parsing it.
pub struct ValueValidator {
    pub value: ast::Value,
}

impl ValueValidator {
    /// Creates a new instance by wrapping a value.
    ///
    /// If the value is a function expression, it is evaluated
    /// recursively.
    pub fn new(value: &ast::Value) -> Result<ValueValidator, ValidationError> {
        match value {
            ast::Value::StringValue(string, span) => Ok(ValueValidator {
                value: StringInterpolator::interpolate(string, span)?,
            }),
            _ => Ok(ValueValidator {
                value: FunctionalEvaluator::new(value).evaluate()?,
            }),
        }
    }

    /// Creates a new type mismatch error for the
    /// value wrapped by this instance.
    fn construct_error(&self, expected_type: &str) -> ValidationError {
        ValidationError::new_type_mismatch_error(
            expected_type,
            ast::describe_value_type(&self.value),
            self.raw(),
            self.span(),
        )
    }

    /// Creates a value parser error
    /// from some other parser error.
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

    /// The wrapped value.
    pub fn value(&self) -> &ast::Value {
        &self.value
    }

    // TODO: Array types might be convenient here, for lists.

    /// Attempts to parse the wrapped value
    /// to a given prisma type.
    pub fn as_type(&self, scalar_type: &PrismaType) -> Result<dml::Value, ValidationError> {
        match scalar_type {
            PrismaType::Int => wrap_value!(self.as_int(), dml::Value::Int, self),
            PrismaType::Float => wrap_value!(self.as_float(), dml::Value::Float, self),
            PrismaType::Decimal => wrap_value!(self.as_decimal(), dml::Value::Decimal, self),
            PrismaType::Boolean => wrap_value!(self.as_bool(), dml::Value::Boolean, self),
            PrismaType::DateTime => wrap_value!(self.as_date_time(), dml::Value::DateTime, self),
            PrismaType::String => wrap_value!(self.as_str(), dml::Value::String, self),
            PrismaType::ConstantLiteral => wrap_value!(self.as_constant_literal(), dml::Value::String, self),
        }
    }

    /// Parses the wrapped value as a given literal type.
    pub fn parse_literal<T: FromStrAndSpan>(&self) -> Result<T, ValidationError> {
        T::from_str_and_span(&self.as_constant_literal()?, self.span())
    }

    /// Accesses the raw string representation
    /// of the wrapped value.
    pub fn raw(&self) -> &str {
        match &self.value {
            ast::Value::StringValue(x, _) => x,
            ast::Value::NumericValue(x, _) => x,
            ast::Value::BooleanValue(x, _) => x,
            ast::Value::ConstantValue(x, _) => x,
            ast::Value::Function(x, _, _) => x,
            ast::Value::Array(_, _) => "(Array)",
            ast::Value::ServerSideFunction(_, _, _, _) => "(Function)",
        }
    }

    /// Accesses the span of the wrapped value.
    pub fn span(&self) -> &ast::Span {
        match &self.value {
            ast::Value::StringValue(_, s) => s,
            ast::Value::NumericValue(_, s) => s,
            ast::Value::BooleanValue(_, s) => s,
            ast::Value::ConstantValue(_, s) => s,
            ast::Value::Function(_, _, s) => s,
            ast::Value::Array(_, s) => s,
            ast::Value::ServerSideFunction(_, _, _, s) => s,
        }
    }

    /// Tries to convert the wrapped value to a Prisma String.
    pub fn as_str(&self) -> Result<String, ValidationError> {
        match &self.value {
            ast::Value::StringValue(value, _) => Ok(value.to_string()),
            _ => Err(self.construct_error("String")),
        }
    }

    /// Tries to convert the wrapped value to a Prisma Integer.
    pub fn as_int(&self) -> Result<i32, ValidationError> {
        match &self.value {
            ast::Value::NumericValue(value, _) => self.wrap_error_from_result(value.parse::<i32>(), "Numeric"),
            _ => Err(self.construct_error("Numeric")),
        }
    }

    /// Tries to convert the wrapped value to a Prisma Float.
    pub fn as_float(&self) -> Result<f32, ValidationError> {
        match &self.value {
            ast::Value::NumericValue(value, _) => self.wrap_error_from_result(value.parse::<f32>(), "Numeric"),
            _ => Err(self.construct_error("Numeric")),
        }
    }

    // TODO: Ask which decimal type to take.
    /// Tries to convert the wrapped value to a Prisma Decimal.
    pub fn as_decimal(&self) -> Result<f32, ValidationError> {
        match &self.value {
            ast::Value::NumericValue(value, _) => self.wrap_error_from_result(value.parse::<f32>(), "Numeric"),
            _ => Err(self.construct_error("Numeric")),
        }
    }

    /// Tries to convert the wrapped value to a Prisma Boolean.
    pub fn as_bool(&self) -> Result<bool, ValidationError> {
        match &self.value {
            ast::Value::BooleanValue(value, _) => self.wrap_error_from_result(value.parse::<bool>(), "Boolean"),
            _ => Err(self.construct_error("Boolean")),
        }
    }

    // TODO: Ask which datetime type to use.
    /// Tries to convert the wrapped value to a Prisma DateTime.
    pub fn as_date_time(&self) -> Result<DateTime<Utc>, ValidationError> {
        match &self.value {
            ast::Value::StringValue(value, _) => {
                self.wrap_error_from_result(value.parse::<DateTime<Utc>>(), "String-Like")
            }
            _ => Err(self.construct_error("String-Like")),
        }
    }

    /// Unwraps the wrapped value as a constant literal..
    pub fn as_constant_literal(&self) -> Result<String, ValidationError> {
        match &self.value {
            ast::Value::ConstantValue(value, _) => Ok(value.to_string()),
            _ => Err(self.construct_error("Constant Literal")),
        }
    }

    /// Unwraps the wrapped value as a constant literal..
    pub fn as_array(&self) -> Result<Vec<ValueValidator>, ValidationError> {
        match &self.value {
            ast::Value::Array(values, _) => {
                let mut validators: Vec<ValueValidator> = Vec::new();

                for value in values {
                    validators.push(ValueValidator::new(value)?);
                }

                Ok(validators)
            }
            _ => Ok(vec![ValueValidator::new(&self.value)?]),
        }
    }
}

pub trait ValueListValidator {
    fn to_str_vec(&self) -> Result<Vec<String>, ValidationError>;
    fn to_literal_vec(&self) -> Result<Vec<String>, ValidationError>;
}

impl ValueListValidator for Vec<ValueValidator> {
    fn to_str_vec(&self) -> Result<Vec<String>, ValidationError> {
        let mut res: Vec<String> = Vec::new();

        for val in self {
            res.push(val.as_str()?);
        }

        Ok(res)
    }

    fn to_literal_vec(&self) -> Result<Vec<String>, ValidationError> {
        let mut res: Vec<String> = Vec::new();

        for val in self {
            res.push(val.as_constant_literal()?);
        }

        Ok(res)
    }
}

impl Into<ast::Value> for dml::Value {
    fn into(self) -> ast::Value {
        (&self).into()
    }
}

impl Into<ast::Value> for &dml::Value {
    fn into(self) -> ast::Value {
        match self {
            dml::Value::Boolean(true) => ast::Value::BooleanValue(String::from("true"), ast::Span::empty()),
            dml::Value::Boolean(false) => ast::Value::BooleanValue(String::from("false"), ast::Span::empty()),
            dml::Value::String(value) => ast::Value::StringValue(value.clone(), ast::Span::empty()),
            dml::Value::ConstantLiteral(value) => ast::Value::ConstantValue(value.clone(), ast::Span::empty()),
            dml::Value::DateTime(value) => ast::Value::ConstantValue(value.to_rfc3339(), ast::Span::empty()),
            dml::Value::Decimal(value) => ast::Value::NumericValue(value.to_string(), ast::Span::empty()),
            dml::Value::Float(value) => ast::Value::NumericValue(value.to_string(), ast::Span::empty()),
            dml::Value::Int(value) => ast::Value::NumericValue(value.to_string(), ast::Span::empty()),
            dml::Value::Expression(name, return_type, args) => {
                ast::Value::ServerSideFunction(
                    name.clone(),
                    *return_type,
                    args.iter().map(|a| a.into()).collect(),
                    ast::Span::empty()
                )
            },
        }
    }
}
