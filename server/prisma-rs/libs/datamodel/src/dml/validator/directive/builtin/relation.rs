use crate::dml;
use crate::dml::validator::directive::{Args, Error, DirectiveValidator};

pub struct RelationDirectiveValidator { }

impl DirectiveValidator<dml::Field> for RelationDirectiveValidator {
    fn directive_name(&self) -> &'static str{ &"relation" }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field) -> Option<Error> {

        if let Ok(name) = args.arg("name").as_str() {
            match field.field_type.clone() {
                dml::FieldType::Relation { to, to_field, name: None, on_delete } => field.field_type = dml::FieldType::Relation { to: to, to_field: to_field, name: Some(name), on_delete: on_delete },
                dml::FieldType::Relation { to, to_field, name: Some(_), on_delete } => return self.error("Relation name already set."),
                _ => return self.error("Invalid field type, not a relation.")
            }
        }

        return None
    }
}