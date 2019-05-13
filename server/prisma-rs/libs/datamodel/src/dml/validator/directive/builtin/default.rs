use crate::dml;
use crate::dml::validator::directive::{error, Args, DirectiveValidator, Error};

pub struct DefaultDirectiveValidator {}

impl<Types: dml::TypePack> DirectiveValidator<dml::Field<Types>, Types> for DefaultDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"default"
    }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field<Types>) -> Option<Error> {
        // TODO: This is most likely duplicate code.
        if let dml::FieldType::Base(scalar_type) = field.field_type {
            match args.default_arg("value").as_type(&scalar_type) {
                // TODO: Here, a default value directive can override the default value syntax sugar.
                Ok(value) => field.default_value = Some(value),
                Err(err) => return Some(err),
            }
        } else {
            return error("Cannot set a default value on a non-scalar field.");
        }

        return None;
    }
}
