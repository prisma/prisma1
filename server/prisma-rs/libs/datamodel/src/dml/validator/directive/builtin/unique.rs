use crate::dml;
use crate::dml::validator::directive::{Args, Error, DirectiveValidator};

pub struct UniqueDirectiveValidator { }

impl<Types: dml::TypePack> DirectiveValidator<dml::Field<Types>, Types> for UniqueDirectiveValidator {
    fn directive_name(&self) -> &'static str{ &"unique" }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Field<Types>) -> Option<Error> {
        obj.is_unique = true;
        return None
    }
}