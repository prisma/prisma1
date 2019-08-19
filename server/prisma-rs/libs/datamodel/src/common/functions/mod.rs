mod traits;
pub use traits::*;
mod builtin;
use crate::ast;
use crate::common::{
    value::{MaybeExpression, ValueValidator},
    PrismaType,
};
use crate::errors::ValidationError;
pub use builtin::*;

// Client side funcs
const BUILTIN_ENV_FUNCTIONAL: builtin::EnvFunctional = builtin::EnvFunctional {};

// Server side funcs
const BUILTIN_NOW_FUNCTIONAL: builtin::ServerSideTrivialFunctional = builtin::ServerSideTrivialFunctional {
    name: "now",
    return_type: PrismaType::DateTime,
};
const BUILTIN_CUID_FUNCTIONAL: builtin::ServerSideTrivialFunctional = builtin::ServerSideTrivialFunctional {
    name: "cuid",
    return_type: PrismaType::String,
};
const BUILTIN_UUID_FUNCTIONAL: builtin::ServerSideTrivialFunctional = builtin::ServerSideTrivialFunctional {
    name: "uuid",
    return_type: PrismaType::String,
};

/// Array of all builtin functionals.
const BUILTIN_FUNCTIONALS: [&'static Functional; 4] = [
    &BUILTIN_ENV_FUNCTIONAL,
    &BUILTIN_NOW_FUNCTIONAL,
    &BUILTIN_CUID_FUNCTIONAL,
    &BUILTIN_UUID_FUNCTIONAL,
];

/// Evaluator for arbitrary expressions.
pub struct FunctionalEvaluator {
    value: ast::Value,
}

impl FunctionalEvaluator {
    /// Wraps a value into a function evaluator.
    pub fn new(value: &ast::Value) -> FunctionalEvaluator {
        FunctionalEvaluator { value: value.clone() }
    }

    /// Evaluates the value wrapped in this instance.
    ///
    /// If the value is of type Function, the corresponding function will
    /// be identified and and executed.
    ///
    /// Otherwise, if the value is a constant, the value is returned as-is.
    pub fn evaluate(&self) -> Result<MaybeExpression, ValidationError> {
        match &self.value {
            ast::Value::Function(name, params, span) => self.evaluate_functional(&name, &params, &span),
            _ => Ok(MaybeExpression::Value(None, self.value.clone())),
        }
    }

    fn evaluate_functional(
        &self,
        name: &str,
        args: &Vec<ast::Value>,
        span: &ast::Span,
    ) -> Result<MaybeExpression, ValidationError> {
        for f in &BUILTIN_FUNCTIONALS {
            if f.name() == name {
                let mut resolved_args: Vec<ValueValidator> = Vec::new();

                for value in args {
                    resolved_args.push(ValueValidator::new(value)?)
                }

                return f.apply(&resolved_args, span);
            }
        }

        return Err(ValidationError::new_function_not_known_error(name, span));
    }
}
