use crate::dml;

pub mod builtin;

pub type Error = dml::validator::value::ValueParserError;
pub type Args<'a> = dml::validator::argument::DirectiveArguments<'a>;

// TODO Narrow to type, enum, field, if possible
pub trait DirectiveValidator<T> {
    fn directive_name(&self) -> &'static str;
    // TODO: Proper error type
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Option<Error>;
}

pub trait TypeDirectiveValidator: DirectiveValidator<dml::Type> {}
pub trait EnumDirectiveValidator: DirectiveValidator<dml::Enum> {}
pub trait FieldDirectiveValidator: DirectiveValidator<dml::Field> {}
