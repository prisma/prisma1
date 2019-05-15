use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

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
            }
            Err(err) => return self.parser_error(&err)
        }
    

        return Ok(());
    }
}
