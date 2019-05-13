use crate::dml;

pub mod builtin;

// TODO: This should not be related to value parsing.
pub type Error = dml::validator::value::ValueParserError;
pub type Args<'a> = dml::validator::argument::DirectiveArguments<'a>;

pub fn error(msg: &str) -> Option<Error> {
    Some(Error::new(String::from(msg), String::from("")))
}

// TODO Narrow to type, enum, field, if possible
pub trait DirectiveValidator<T, Types: dml::TypePack> {
    fn directive_name(&self) -> &'static str;
    // TODO: Proper error type
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Option<Error>;
}

pub trait ModelDirectiveValidator<Types: dml::TypePack>: DirectiveValidator<dml::Model<Types>, Types> {}
pub trait EnumDirectiveValidator<Types: dml::TypePack>: DirectiveValidator<dml::Enum<Types>, Types> {}
pub trait FieldDirectiveValidator<Types: dml::TypePack>: DirectiveValidator<dml::Field<Types>, Types> {}
