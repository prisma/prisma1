use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

pub struct PrimaryDirectiveValidator {}

impl DirectiveValidator<dml::Field> for PrimaryDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"primary"
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Field) -> Result<(), Error> {
        let mut id_info = dml::IdInfo {
            strategy: dml::IdStrategy::Auto,
            sequence: None,
        };

        if let Ok(arg) = args.arg("name") {
            if let Ok(strategy) = arg.as_constant_literal() {
                match strategy.parse::<dml::IdStrategy>() {
                    Ok(strategy) => id_info.strategy = strategy,
                    Err(err) => return self.parser_error(&err),
                }
            }
        }

        obj.id_info = Some(id_info);

        return Ok(());
    }
}
