use crate::ast;
use crate::dml;
use crate::errors::{DirectiveValidationError, ErrorWithSpan};

pub mod builtin;

pub type Error = DirectiveValidationError;
pub type Args<'a> = dml::validator::argument::DirectiveArguments<'a>;

// TODO Narrow to type, enum, field, if possible
pub trait DirectiveValidator<T> {
    fn directive_name(&self) -> &'static str;
    // TODO: Proper error type
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Result<(), Error>;

    fn error(&self, msg: &str, span: &ast::Span) -> Result<(), Error> {
        Err(Error::new(msg, self.directive_name(), span))
    }

    fn parser_error(&self, err: &ErrorWithSpan) -> Result<(), Error> {
        Err(Error::new(&format!("{}", err), self.directive_name(), &err.span()))
    }
}

pub trait ModelDirectiveValidator: DirectiveValidator<dml::Model> {}
pub trait EnumDirectiveValidator: DirectiveValidator<dml::Enum> {}
pub trait FieldDirectiveValidato: DirectiveValidator<dml::Field> {}
