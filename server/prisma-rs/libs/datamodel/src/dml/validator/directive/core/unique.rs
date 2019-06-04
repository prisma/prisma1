use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::{ast, dml};

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

    fn serialize(&self, field: &dml::Field, datamodel: &dml::Datamodel) -> Result<Option<ast::Directive>, Error> {
        if field.is_unique {
            return Ok(Some(ast::Directive::new(self.directive_name(), vec![])));
        }

        Ok(None)
    }
}
