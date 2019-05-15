use crate::dml;
use std::fmt;
use crate::ast;

pub mod builtin;

#[derive(Debug)]
pub struct DirectiveValidationError {
    pub message: String,
    pub directive_name: String,
    pub span: ast::Span
}

impl DirectiveValidationError {
    pub fn new(message: &str, directive_name: &str, span: &ast::Span) -> DirectiveValidationError {
        DirectiveValidationError {
            message: String::from(message),
            directive_name: String::from(directive_name),
            span: span.clone()
        }
    }
}

impl fmt::Display for DirectiveValidationError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", self.message)
    }
}

impl std::error::Error for DirectiveValidationError {
    fn description(&self) -> &str {
        self.message.as_str()
    }

    fn cause(&self) -> Option<&std::error::Error> {
        None
    }
}

pub type Error = DirectiveValidationError;
pub type Args<'a> = dml::validator::argument::DirectiveArguments<'a>;

// TODO Narrow to type, enum, field, if possible
pub trait DirectiveValidator<T> {
    fn directive_name(&self) -> &'static str;
    // TODO: Proper error type
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Option<Error>;

    fn error(&self, msg: &str, span: &ast::Span) -> Option<Error> {
        Some(Error::new(msg, self.directive_name(), span))
    }

    fn parser_error(&self, err: &dml::validator::value::ValueParserError) -> Option<Error> {
        Some(Error::new(&err.message, self.directive_name(), &err.span))
    }
}

pub trait ModelDirectiveValidator: DirectiveValidator<dml::Model> {}
pub trait EnumDirectiveValidator: DirectiveValidator<dml::Enum> {}
pub trait FieldDirectiveValidato: DirectiveValidator<dml::Field> {}
