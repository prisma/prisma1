use crate::dml;
use crate::dml::validator::directive::{Args, Error, DirectiveValidator};

pub struct PrimaryDirectiveValidator { }

impl DirectiveValidator<dml::Field> for PrimaryDirectiveValidator {
    fn directive_name(&self) -> &'static str{ &"primary" }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Field) -> Option<Error> {
        obj.is_id = true;

        if let Ok(strategy) = args.arg("name").as_constant_literal() {
            match strategy.parse::<dml::IdStrategy>() {
                Ok(strategy) => obj.id_strategy = Some(strategy),
                Err(err) => return Some(err)
            }
        }

        return None
    }
}