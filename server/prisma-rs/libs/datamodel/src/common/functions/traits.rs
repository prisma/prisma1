use crate::ast;
use crate::common::value::ValueValidator;
use crate::errors::ValidationError;

/// Trait for functionals.
pub trait Functional {
    fn name(&self) -> &str;
    fn apply(&self, values: &Vec<ValueValidator>, span: &ast::Span) -> Result<ast::Value, ValidationError>;

    fn check_arg_count(
        &self,
        values: &Vec<ValueValidator>,
        count: usize,
        span: &ast::Span,
    ) -> Result<(), ValidationError> {
        if values.len() == count {
            Ok(())
        } else {
            Err(ValidationError::new_argument_count_missmatch_error(
                self.name(),
                count,
                values.len(),
                span,
            ))
        }
    }
}
