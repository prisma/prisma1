use crate::ast;
use crate::common::value::{MaybeExpression, ValueValidator};
use crate::errors::ValidationError;

/// Trait for functions which can be accessed from the datamodel.
pub trait Functional {
    /// Gets the name of the function.
    fn name(&self) -> &str;

    /// Applies the function to the given arguments and returns the result.
    fn apply(&self, values: &[ValueValidator], span: ast::Span) -> Result<MaybeExpression, ValidationError>;

    /// Internal: Shorthand to check the count of arguments, and raise an error if applicable.
    fn check_arg_count(
        &self,
        values: &[ValueValidator],
        count: usize,
        span: ast::Span,
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
