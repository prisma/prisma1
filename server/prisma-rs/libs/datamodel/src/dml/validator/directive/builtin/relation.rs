use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

pub struct RelationDirectiveValidator {}

impl DirectiveValidator<dml::Field> for RelationDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"relation"
    }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field) -> Result<(), Error> {
        if let Ok(name) = args.arg("name")?.as_str() {
            match &mut field.field_type {
                // TODO: Check if name is already set.
                dml::FieldType::Relation(relation_info) => relation_info.name = Some(name),
                _ => return self.error("Invalid field type, not a relation.", &args.span()),
            }
        }

        return Ok(());
    }
}
