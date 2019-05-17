use crate::ast;
use crate::dml;
use crate::errors::ValidationError;

pub mod builtin;

pub type Error = ValidationError;
pub type Args<'a> = dml::validator::argument::DirectiveArguments<'a>;

pub trait DirectiveValidator<T> {
    fn directive_name(&self) -> &'static str;
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Result<(), Error>;

    fn error(&self, msg: &str, span: &ast::Span) -> Result<(), Error> {
        Err(ValidationError::new_directive_validation_error(
            msg,
            self.directive_name(),
            span,
        ))
    }

    fn parser_error(&self, err: &ValidationError) -> Result<(), Error> {
        Err(ValidationError::new_directive_validation_error(
            &format!("{}", err),
            self.directive_name(),
            &err.span(),
        ))
    }
}

pub trait ModelDirectiveValidator: DirectiveValidator<dml::Model> {}
pub trait EnumDirectiveValidator: DirectiveValidator<dml::Enum> {}
pub trait FieldDirectiveValidato: DirectiveValidator<dml::Field> {}
