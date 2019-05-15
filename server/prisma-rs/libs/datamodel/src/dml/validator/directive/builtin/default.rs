use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

pub struct DefaultDirectiveValidator {}

impl DirectiveValidator<dml::Field> for DefaultDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"default"
    }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field) -> Result<(), Error> {
        // TODO: This is most likely duplicate code.
        if let dml::FieldType::Base(scalar_type) = field.field_type {
            match args.default_arg("value")?.as_type(&scalar_type) {
                // TODO: Here, a default value directive can override the default value syntax sugar.
                Ok(value) => field.default_value = Some(value),
                Err(err) => return self.parser_error(&err),
            }
        } else {
            return self.error("Cannot set a default value on a non-scalar field.", &args.span());
        }

        return Ok(());
    }
}
