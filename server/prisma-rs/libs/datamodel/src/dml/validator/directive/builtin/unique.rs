use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

pub struct UniqueDirectiveValidator {}

impl DirectiveValidator<dml::Field> for UniqueDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"unique"
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Field) -> Option<Error> {
        obj.is_unique = true;
        return None;
    }
}
