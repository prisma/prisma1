use super::Functional;
use crate::ast;
use crate::common::value::ValueValidator;
use crate::errors::ValidationError;

/// Environment variable interpolating function (`env(...)`).
pub struct EnvFunctional {}

impl Functional for EnvFunctional {
    fn name(&self) -> &str {
        "env"
    }
    fn apply(&self, values: &Vec<ValueValidator>, span: &ast::Span) -> Result<ast::Value, ValidationError> {
        self.check_arg_count(values, 1, span)?;

        let var_wrapped = &values[0];
        let var_name = var_wrapped.as_str()?;
        if let Ok(var) = std::env::var(&var_name) {
            Ok(ast::Value::StringValue(var, span.clone()))
        } else {
            Err(ValidationError::new_functional_evaluation_error(
                &format!("Environment variable not found: {}", var_name),
                var_wrapped.span(),
            ))
        }
    }
}
