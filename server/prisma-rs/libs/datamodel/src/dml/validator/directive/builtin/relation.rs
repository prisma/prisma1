use crate::dml;
use crate::dml::validator::directive::{Args, Error, DirectiveValidator, error};

pub struct RelationDirectiveValidator { }

impl<Types: dml::TypePack> DirectiveValidator<dml::Field<Types>, Types> for RelationDirectiveValidator {
    fn directive_name(&self) -> &'static str{ &"relation" }
    fn validate_and_apply(&self, args: &Args, field: &mut dml::Field<Types>) -> Option<Error> {

        if let Ok(name) = args.arg("name").as_str() {
            match &mut field.field_type {
                // TODO: Check if name is already set.
                dml::FieldType::Relation(relation_info) => relation_info.name = Some(name),
                _ => return error("Invalid field type, not a relation.")
            }
        }

        return None
    }
}