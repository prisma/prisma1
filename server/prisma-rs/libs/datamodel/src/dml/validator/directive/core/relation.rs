use crate::common::names::DefaultNames;
use crate::common::value::ValueListValidator;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::{ast, dml};

/// Prismas builtin `@relation` directive.
pub struct RelationDirectiveValidator {}

impl DirectiveValidator<dml::Field> for RelationDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"relation"
    }
    fn validate_and_apply(&self, args: &mut Args, field: &mut dml::Field) -> Result<(), Error> {
        if let dml::FieldType::Relation(relation_info) = &mut field.field_type {
            if let Ok(name_arg) = args.default_arg("name") {
                let name = name_arg.as_str()?;
                if name.len() == 0 {
                    return self.error("A relation cannot have an empty name.", &name_arg.span());
                }
                relation_info.name = name;
            }

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

    fn serialize(&self, field: &dml::Field, datamodel: &dml::Datamodel) -> Result<Option<ast::Directive>, Error> {
        if let dml::FieldType::Relation(relation_info) = &field.field_type {
            let mut args = Vec::new();

            // These unwraps must be safe.
            let parent_model = datamodel.find_model_by_field_ref(field).unwrap();
            let related_model = datamodel
                .find_model(&relation_info.to)
                .expect(&format!("Related model not found: {}.", relation_info.to));
            let mut all_related_ids: Vec<&String> = related_model.id_field_names().collect();

            if !relation_info.name.is_empty()
                && relation_info.name != DefaultNames::relation_name(&relation_info.to, &parent_model.name)
            {
                args.push(ast::Argument::new_string("", &relation_info.name));
            }

            // We only add the references arg,
            // if we have references
            // and we do only reference the IDs, which is the default case.
            if relation_info.to_fields.len() > 0 && relation_info.to_fields.clone().sort() != all_related_ids.sort() {
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
