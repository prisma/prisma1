use crate::common::value::ValueListValidator;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::{ast, dml};

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

    fn serialize(&self, field: &dml::Field) -> Result<Option<ast::Directive>, Error> {
        if let dml::FieldType::Relation(relation_info) = &field.field_type {
            let mut args = Vec::new();

            if let Some(name) = &relation_info.name {
                args.push(ast::Argument::new_string("name", &name));
            }

            let mut related_fields: Vec<ast::Value> = Vec::new();

            for related_field in &relation_info.to_fields {
                related_fields.push(ast::Value::StringValue(related_field.clone(), ast::Span::empty()));
            }

            args.push(ast::Argument::new_array("references", related_fields));

            return Ok(Some(ast::Directive::new(self.directive_name(), args)));
        }

        Ok(None)
    }
}
