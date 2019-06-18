use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::{ast, dml};

/// Prismas builtin `@default` directive.
pub struct DefaultDirectiveValidator {}

impl DirectiveValidator<dml::Field> for DefaultDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"default"
    }
    fn validate_and_apply(&self, args: &mut Args, field: &mut dml::Field) -> Result<(), Error> {
        // If we allow list default values, we need to adjust the types below properly for that case.
        if field.arity == dml::FieldArity::List {
            return self.error("Cannot set a default value on list field.", &args.span());
        }

        if let dml::FieldType::Base(scalar_type) = &field.field_type {
            match args.default_arg("value")?.as_type(&scalar_type) {
                // TODO: Here, a default value directive can override the default value syntax sugar.
                Ok(value) => field.default_value = Some(value),
                Err(err) => return self.parser_error(&err),
            }
        } else if let dml::FieldType::Enum(_) = &field.field_type {
            match args.default_arg("value")?.as_constant_literal() {
                // TODO: We should also check if this value is a valid enum value.
                Ok(value) => field.default_value = Some(dml::Value::ConstantLiteral(value)),
                Err(err) => return self.parser_error(&err),
            }
        } else {
            return self.error("Cannot set a default value on a relation field.", &args.span());
        }

        return Ok(());
    }

    fn serialize(&self, field: &dml::Field, _datamodel: &dml::Datamodel) -> Result<Option<ast::Directive>, Error> {
        if let Some(default_value) = &field.default_value {
            return Ok(Some(ast::Directive::new(
                self.directive_name(),
                vec![ast::Argument::new("", default_value.into())],
            )));
        }

        Ok(None)
    }
}
