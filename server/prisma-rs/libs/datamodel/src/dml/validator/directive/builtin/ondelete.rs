use crate::dml;
use crate::dml::validator::directive::{Args, Error, DirectiveValidator};

pub struct OnDeleteDirectiveValidator { }

impl DirectiveValidator<dml::Field> for OnDeleteDirectiveValidator {
    fn directive_name(&self) -> &'static str{ &"onDelete" }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field) -> Option<Error> {

        if let Ok(strategy) = args.arg("strategy").as_constant_literal() {
            match (strategy.parse::<dml::OnDeleteStrategy>(), field.field_type.clone()) {
                (Ok(strategy), dml::FieldType::Relation { to, to_field, name: name, on_delete }) => field.field_type = dml::FieldType::Relation { to: to, to_field: to_field, name: name, on_delete: strategy },
                (Err(err), _) => return Some(err),
                (Ok(_), _) => return self.error("Invalid field type, not a relation.")
            }
        }

        return None
    }
}