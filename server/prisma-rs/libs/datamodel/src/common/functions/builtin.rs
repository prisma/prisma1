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
            Ok(MaybeExpression::Value(ast::Value::Any(var, span.clone())))
        } else {
            Err(ValidationError::new_functional_evaluation_error(
                &format!("Environment variable not found: \"{}\".", var_name),
                var_wrapped.span(),
            ))
        }
    }
}

/// Shallow implementation for trivial server side functionals.
pub struct ServerSideTrivialFunctional {
    // Needed for const initializer.
    pub(crate) name: &'static str,
    pub(crate) return_type: PrismaType,
}

impl ServerSideTrivialFunctional {
    pub fn new(name: &'static str, return_type: PrismaType) -> ServerSideTrivialFunctional {
        ServerSideTrivialFunctional { name, return_type }
    }
}

impl Functional for ServerSideTrivialFunctional {
    fn name(&self) -> &str {
        self.name
    }
    fn apply(&self, values: &Vec<ValueValidator>, span: &ast::Span) -> Result<MaybeExpression, ValidationError> {
        self.check_arg_count(values, 0, span)?;

        Ok(server_functional_with(self.name(), self.return_type, span))
    }
}
