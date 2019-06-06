use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::{ast, dml};

/// Prismas builtin `@updatedAt` directive.
pub struct UpdatedAtDirectiveValidator {}

impl DirectiveValidator<dml::Field> for UpdatedAtDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"updatedAt"
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Field) -> Result<(), Error> {
        if obj.field_type != dml::FieldType::Base(dml::ScalarType::DateTime) {
            return self.error(
                "Fields that are marked with @updatedAt must be of type DateTime.",
                &args.span(),
            );
        }

        if obj.arity == dml::FieldArity::List {
            return self.error("Fields that are marked with @updatedAt can not be lists.", &args.span());
        }

        obj.is_updated_at = true;

        return Ok(());
    }

    fn serialize(&self, field: &dml::Field, _datamodel: &dml::Datamodel) -> Result<Option<ast::Directive>, Error> {
        if field.is_updated_at {
            Ok(Some(ast::Directive::new(self.directive_name(), Vec::new())))
        } else {
            Ok(None)
        }
    }
}
