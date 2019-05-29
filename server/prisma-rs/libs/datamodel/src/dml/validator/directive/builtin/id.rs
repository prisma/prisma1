use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

/// Prismas builtin `@primary` directive.
pub struct IdDirectiveValidator {}

impl DirectiveValidator<dml::Field> for IdDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"id"
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Field) -> Result<(), Error> {
        let mut id_info = dml::IdInfo {
            strategy: dml::IdStrategy::Auto,
            sequence: None,
        };

        if obj.arity != dml::FieldArity::Required {
            return self.error("Fields that are marked as id must be required.", &args.span());
        }

        if let Ok(arg) = args.arg("strategy") {
            id_info.strategy = arg.parse_literal::<dml::IdStrategy>()?
        }

        obj.id_info = Some(id_info);

        return Ok(());
    }
}
