use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::{ast, dml};

/// Prismas builtin `@default` directive.
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

    fn serialize(&self, field: &dml::Field, datamodel: &dml::Datamodel) -> Result<Option<ast::Directive>, Error> {
        if let Some(default_value) = &field.default_value {
            return Ok(Some(ast::Directive::new(
                self.directive_name(),
                vec![ast::Argument::new("", default_value.into())],
            )));
        }

        Ok(None)
    }
}
