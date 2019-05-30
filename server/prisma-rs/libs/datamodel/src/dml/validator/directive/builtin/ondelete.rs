use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::{ast, dml};

/// Prismas builtin `@db` directive.
pub struct OnDeleteDirectiveValidator {}

impl DirectiveValidator<dml::Field> for OnDeleteDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"onDelete"
    }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field) -> Result<(), Error> {
        match args.default_arg("strategy")?.parse_literal::<dml::OnDeleteStrategy>() {
            Ok(strategy) => match &mut field.field_type {
                dml::FieldType::Relation(relation_info) => relation_info.on_delete = strategy,
                _ => return self.error("Invalid field type, not a relation.", &args.span()),
            },
            Err(err) => return self.parser_error(&err),
        }

        return Ok(());
    }

    fn serialize(&self, obj: &dml::Field) -> Result<Option<ast::Directive>, Error> {
        if let dml::FieldType::Relation(relation_info) = &obj.field_type {
            if relation_info.on_delete != dml::OnDeleteStrategy::None {
                return Ok(Some(ast::Directive::new(
                    self.directive_name(),
                    vec![ast::Argument::new_constant("", &relation_info.on_delete.to_string())],
                )));
            }
        }

        Ok(None)
    }
}
