use crate::common::value::ValueListValidator;
use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

/// Prismas builtin `@relation` directive.
pub struct RelationDirectiveValidator {}

impl DirectiveValidator<dml::Field> for RelationDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"relation"
    }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field) -> Result<(), Error> {
        if let dml::FieldType::Relation(relation_info) = &mut field.field_type {
            // TODO: Check if name is already set.
            if let Ok(name) = args.arg("name") {
                relation_info.name = Some(name.as_str()?);
            }
            // TODO: Check if fields are valid.
            if let Ok(related_fields) = args.arg("references") {
                relation_info.to_fields = related_fields.as_array()?.to_literal_vec()?;
            }
            return Ok(());
        } else {
            return self.error("Invalid field type, not a relation.", &args.span());
        }
    }
}
