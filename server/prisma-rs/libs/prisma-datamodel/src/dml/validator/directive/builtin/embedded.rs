use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

pub struct EmbeddedDirectiveValidator {}

impl DirectiveValidator<dml::Type> for EmbeddedDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"embedded"
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Type) -> Option<Error> {
        obj.is_embedded = true;
        return None;
    }
}
