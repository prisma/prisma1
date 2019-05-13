use crate::dml;
use crate::dml::validator::directive::{error, Args, DirectiveValidator, Error};

pub struct OnDeleteDirectiveValidator {}

impl<Types: dml::TypePack> DirectiveValidator<dml::Field<Types>, Types> for OnDeleteDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"onDelete"
    }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field<Types>) -> Option<Error> {
        if let Ok(strategy) = args.arg("strategy").as_constant_literal() {
            match (strategy.parse::<dml::OnDeleteStrategy>(), &mut field.field_type) {
                (Ok(strategy), dml::FieldType::Relation(relation_info)) => relation_info.on_delete = strategy,
                (Err(err), _) => return Some(err),
                _ => return error("Invalid field type, not a relation."),
            }
        }

        return None;
    }
}
