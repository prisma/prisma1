use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

/// Prismas builtin `@unique` directive.
pub struct UniqueDirectiveValidator {}

impl DirectiveValidator<dml::Field> for UniqueDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"unique"
    }
    fn validate_and_apply(&self, _args: &Args, obj: &mut dml::Field) -> Result<(), Error> {
        obj.is_unique = true;
        return Ok(());
    }
}
