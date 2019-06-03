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
            if let Ok(name) = args.default_arg("name") {
                relation_info.name = Some(name.as_str()?);
            }
            // TODO: Check if fields are valid.
            if let Ok(related_fields) = args.arg("references") {
                relation_info.to_fields = related_fields.as_array()?.to_literal_vec()?;
            }

            if let Ok(on_delete) = args.arg("onDelete") {
                relation_info.on_delete = on_delete.parse_literal::<dml::OnDeleteStrategy>()?;
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
                args.push(ast::Argument::new_string("", &name));
            }

            if relation_info.to_fields.len() > 0 {
                let mut related_fields: Vec<ast::Value> = Vec::new();

                for related_field in &relation_info.to_fields {
                    related_fields.push(ast::Value::ConstantValue(related_field.clone(), ast::Span::empty()));
                }

                args.push(ast::Argument::new_array("references", related_fields));
            }

            if relation_info.on_delete != dml::OnDeleteStrategy::None {
                args.push(ast::Argument::new_constant(
                    "onDelete",
                    &relation_info.on_delete.to_string(),
                ));
            }

            if args.len() > 0 {
                return Ok(Some(ast::Directive::new(self.directive_name(), args)));
            }
        }

        Ok(None)
    }
}
