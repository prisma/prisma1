use super::Functional;
use crate::ast;
use crate::common::{
    value::{MaybeExpression, ValueValidator},
    PrismaType, PrismaValue,
};
use crate::errors::ValidationError;

fn server_functional_with(name: &str, return_type: PrismaType, span: &ast::Span) -> MaybeExpression {
    MaybeExpression::Expression(
        PrismaValue::Expression(String::from(name), return_type, vec![]),
        span.clone(),
    )
}

/// Environment variable interpolating function (`env(...)`).
pub struct EnvFunctional {}

impl Functional for EnvFunctional {
    fn name(&self) -> &str {
        "env"
    }
    fn apply(&self, values: &Vec<ValueValidator>, span: &ast::Span) -> Result<MaybeExpression, ValidationError> {
        self.check_arg_count(values, 1, span)?;

        let var_wrapped = &values[0];
        let var_name = var_wrapped.as_str()?;
        if let Ok(var) = std::env::var(&var_name) {
            Ok(MaybeExpression::Value(ast::Value::StringValue(var, span.clone())))
        } else {
            Err(ValidationError::new_functional_evaluation_error(
                &format!("Environment variable not found: {}", var_name),
                var_wrapped.span(),
            ))
        }
    }
}

/// Server side now function (`now()`).
pub struct NowFunctional {}

impl Functional for NowFunctional {
    fn name(&self) -> &str {
        "now"
    }
    fn apply(&self, values: &Vec<ValueValidator>, span: &ast::Span) -> Result<MaybeExpression, ValidationError> {
        self.check_arg_count(values, 0, span)?;

        Ok(server_functional_with(self.name(), PrismaType::DateTime, span))
    }
}

/// Server side cuid function (`cuid()`).
pub struct CuidFunctional {}

impl Functional for CuidFunctional {
    fn name(&self) -> &str {
        "cuid"
    }
    fn apply(&self, values: &Vec<ValueValidator>, span: &ast::Span) -> Result<MaybeExpression, ValidationError> {
        self.check_arg_count(values, 0, span)?;

        Ok(server_functional_with(self.name(), PrismaType::String, span))
    }
}
